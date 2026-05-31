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

# Lunghezza massima del testo inviato al modello (in caratteri)
MAX_LENGTH = 512
