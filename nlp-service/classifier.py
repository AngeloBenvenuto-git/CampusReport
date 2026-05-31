# Classificatore zero-shot per le categorie di segnalazione del campus

import logging
from transformers import pipeline
from config import MODEL_NAME, CATEGORIES, MAX_LENGTH
from models import ClassifyResponse, AlternativaCategoria

logger = logging.getLogger(__name__)


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

    def is_loaded(self) -> bool:
        """Restituisce True se il modello è stato caricato correttamente."""
        return self._pipeline is not None
