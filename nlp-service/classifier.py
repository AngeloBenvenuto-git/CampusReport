# Classificatore zero-shot per le categorie di segnalazione del campus

import logging
import re
from transformers import pipeline
from config import (
    MODEL_NAME,
    CATEGORIES,
    MAX_LENGTH,
    URGENCY_LEVELS,
    URGENCY_HYPOTHESIS_TEMPLATE,
)
from models import ClassifyResponse, AlternativaCategoria, UrgenzaResponse

logger = logging.getLogger(__name__)

# Parole chiave che indicano un'emergenza immediata (urgenza 5)
KEYWORDS_URGENZA_5 = [
    "incendio", "fuoco", "fiamme", "brucia", "bruciato",
    "esplosione", "esploso", "bomba",
    "gas", "fuga di gas", "odore di gas",
    "allagamento", "allaga", "acqua ovunque", "sommerso",
    "cortocircuito", "scintille", "fulmini",
    "ferito", "infortunio", "sangue", "pronto soccorso",
    "evacuazione", "sgombero", "pericolo vita",
    "crollo", "crolla", "crollato", "cedimento",
]

# Parole chiave che indicano un problema grave (urgenza 4)
KEYWORDS_URGENZA_4 = [
    "completamente rotto", "completamente inutilizzabile",
    "nessuno può entrare", "accesso bloccato",
    "buio totale", "senza corrente", "blackout",
    "lezioni sospese", "aula chiusa", "locale chiuso",
    "caldo insopportabile", "freddo insopportabile",
]

# Parole chiave che indicano un problema minore (urgenza 1)
KEYWORDS_URGENZA_1 = [
    "estetico", "quando possibile", "non urgente",
    "piccolo problema", "lieve", "leggermente",
    "graffi", "macchie", "vernice", "colore",
]

# Negazioni che, se presenti poco prima di una keyword, ne annullano il significato
NEGAZIONI = [
    "non", "nessun", "nessuna", "senza",
    "non c'è", "non ci sono", "assenza di",
    "non vi è", "non esiste",
]

# Numero di parole prima della keyword in cui cercare una negazione
FINESTRA_NEGAZIONE = 3


def _normalizza(testo: str) -> str:
    """Porta il testo in minuscolo e uniforma gli apostrofi tipografici."""
    return testo.lower().replace("’", "'")


def controlla_negazione(testo: str, keyword: str) -> bool:
    """Restituisce True se la keyword è negata in tutte le sue occorrenze nel testo.

    Per ogni occorrenza della keyword guarda le FINESTRA_NEGAZIONE parole
    precedenti (senza attraversare la fine di una frase) e verifica se
    contengono una delle NEGAZIONI. Basta una occorrenza non negata
    perché la keyword sia considerata valida.
    """
    occorrenze = _trova_keyword(testo, keyword)
    if not occorrenze:
        return False
    for inizio in occorrenze:
        # Considera solo la frase corrente: "Non so. C'è un incendio" non è una negazione
        precedente = re.split(r"[.!?;:\n]", testo[:inizio])[-1]
        parole = [p.strip(",()\"") for p in precedente.split()][-FINESTRA_NEGAZIONE:]
        finestra = " ".join(parole)
        negata = any(p in NEGAZIONI for p in parole) or any(
            " " in neg and neg in finestra for neg in NEGAZIONI
        )
        if not negata:
            return False
    return True


def _trova_keyword(testo: str, keyword: str) -> list[int]:
    """Restituisce le posizioni in cui la keyword compare all'inizio di una parola.

    Il match è per prefisso (es. "allaga" trova "allagato"), ma non a metà
    parola (es. "gas" non trova "vegas").
    """
    pattern = r"(?<!\w)" + re.escape(keyword)
    return [m.start() for m in re.finditer(pattern, testo)]


def _keyword_non_negata(testo: str, keywords: list[str]) -> bool:
    """True se almeno una keyword della lista compare nel testo senza negazione."""
    return any(
        _trova_keyword(testo, kw) and not controlla_negazione(testo, kw)
        for kw in keywords
    )


class NLPClassifier:
    """Wrapper del pipeline zero-shot di HuggingFace.

    Il modello viene caricato una sola volta all'avvio per evitare
    il costo di inizializzazione ad ogni richiesta.
    """

    def __init__(self) -> None:
        self._pipeline = None
        # Descrizioni testuali usate come candidate_labels per il modello
        self._candidate_labels: list[str] = list(CATEGORIES.values())
        # Mappa inversa: descrizione → chiave enum (es. "problema elettrico..." → "ELETTRICO")
        self._label_to_key: dict[str, str] = {v: k for k, v in CATEGORIES.items()}
        # Descrizioni dei livelli di urgenza e mappa inversa descrizione → livello (1-5)
        self._urgency_labels: list[str] = list(URGENCY_LEVELS.values())
        self._urgency_label_to_level: dict[str, int] = {v: k for k, v in URGENCY_LEVELS.items()}

    def load(self) -> None:
        """Carica il pipeline zero-shot. Da chiamare una sola volta all'avvio."""
        logger.info("Caricamento modello: %s", MODEL_NAME)
        self._pipeline = pipeline(
            "zero-shot-classification",
            model=MODEL_NAME,
            device=-1,  # CPU; impostare 0 per GPU
        )
        logger.info("Modello caricato con successo")

    def classify(self, testo: str) -> ClassifyResponse:
        """Classifica il testo tra le categorie definite in config.CATEGORIES.

        Tronca l'input a MAX_LENGTH caratteri prima di passarlo al modello.
        Restituisce la categoria con confidenza più alta e le alternative ordinate.
        """
        risultato = self._pipeline(testo[:MAX_LENGTH], self._candidate_labels)

        etichette: list[str] = risultato["labels"]
        punteggi: list[float] = risultato["scores"]

        # Mappa l'etichetta descrittiva alla chiave enum corrispondente
        categoria_principale = self._label_to_key[etichette[0]]
        confidenza = round(punteggi[0], 4)

        alternative = [
            AlternativaCategoria(
                categoria=self._label_to_key[etichetta],
                score=round(punteggio, 4),
            )
            for etichetta, punteggio in zip(etichette[1:], punteggi[1:])
        ]

        logger.debug(
            "Classificazione completata: categoria=%s, confidenza=%.4f",
            categoria_principale,
            confidenza,
        )

        return ClassifyResponse(
            categoria=categoria_principale,
            confidenza=confidenza,
            alternative=alternative,
        )

    def classify_urgenza(self, testo: str) -> UrgenzaResponse:
        """Stima l'urgenza (1-5) del testo con un approccio ibrido keyword + NLP.

        Prima cerca parole chiave di emergenza (5), gravità (4) e problemi minori (1),
        ignorando quelle di livello 5 e 4 precedute da una negazione. Se nessuna
        keyword è decisiva, usa il modello zero-shot sui livelli di config.URGENCY_LEVELS.
        """
        testo_norm = _normalizza(testo)

        if _keyword_non_negata(testo_norm, KEYWORDS_URGENZA_5):
            return UrgenzaResponse(urgenza=5, confidenza=0.95, descrizione="emergenza immediata")

        if _keyword_non_negata(testo_norm, KEYWORDS_URGENZA_4):
            return UrgenzaResponse(urgenza=4, confidenza=0.90, descrizione="problema grave")

        if any(_trova_keyword(testo_norm, kw) for kw in KEYWORDS_URGENZA_1):
            return UrgenzaResponse(urgenza=1, confidenza=0.85, descrizione="problema minore")

        risultato = self._pipeline(
            testo[:MAX_LENGTH],
            candidate_labels=self._urgency_labels,
            hypothesis_template=URGENCY_HYPOTHESIS_TEMPLATE,
        )

        label_vincente: str = risultato["labels"][0]
        urgenza = self._urgency_label_to_level[label_vincente]
        confidenza = round(risultato["scores"][0], 4)

        logger.debug(
            "Stima urgenza completata: urgenza=%d, confidenza=%.4f",
            urgenza,
            confidenza,
        )

        return UrgenzaResponse(
            urgenza=urgenza,
            confidenza=confidenza,
            descrizione=label_vincente,
        )

    def is_loaded(self) -> bool:
        """Restituisce True se il modello è stato caricato correttamente."""
        return self._pipeline is not None
