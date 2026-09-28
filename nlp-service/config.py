# Configurazione del microservizio NLP di CampusReport

# Modello HuggingFace per la classificazione zero-shot (multilingue, supporta italiano)
MODEL_NAME = "MoritzLaurer/mDeBERTa-v3-base-mnli-xnli"

# Porta su cui avvia il server
PORT = 8000

# Categorie con le relative descrizioni testuali usate come candidate_labels per il modello zero-shot.
# Le chiavi corrispondono ai valori dell'enum Categoria del backend Spring.
# Le descrizioni sono in italiano per migliorare la classificazione.
CATEGORIES = {
    "ELETTRICO": "problema elettrico impianto luce corrente",
    "WIFI": "problema rete internet wifi connessione",
    "IDRAULICO": "problema idraulico acqua perdita bagno",
    "ATTREZZATURA": "attrezzatura rotta danneggiata proiettore computer",
    "ALTRO": "altro problema generico",
}

# Livelli di urgenza (1 = minima, 5 = massima) con le descrizioni usate come candidate_labels
# per la stima zero-shot dell'urgenza di una segnalazione.
URGENCY_LEVELS = {
    5: "emergenza immediata pericolo sicurezza incendio allagamento gas ferito evacuazione",
    4: "problema grave blocca completamente attività aula inutilizzabile danno esteso",
    3: "problema importante rallenta attività disagio significativo malfunzionamento",
    2: "problema moderato lieve disagio non urgente può aspettare funziona parzialmente",
    1: "problema minore estetico comfort bassa priorità non influenza attività",
}

# Template dell'ipotesi NLI usato per la stima dell'urgenza
URGENCY_HYPOTHESIS_TEMPLATE = "Questo è un caso di {}."

# Lunghezza massima del testo inviato al modello (in caratteri)
MAX_LENGTH = 512
