# Entry point del microservizio NLP di CampusReport

import logging
from contextlib import asynccontextmanager

import uvicorn
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from classifier import NLPClassifier
from config import MODEL_NAME, PORT
from models import ClassifyRequest, ClassifyResponse, HealthResponse

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Istanza globale del classificatore; il modello viene caricato nel lifespan
nlp_classifier = NLPClassifier()


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Carica il modello all'avvio e lo scarica allo spegnimento."""
    nlp_classifier.load()
    yield
    logger.info("Servizio NLP in chiusura")


app = FastAPI(
    title="CampusReport NLP Service",
    version="1.0.0",
    lifespan=lifespan,
)

# CORS: accetta richieste dal frontend Angular e dal backend Spring
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:4200", "http://localhost:8080"],
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.post("/classify", response_model=ClassifyResponse)
async def classify(request: ClassifyRequest) -> ClassifyResponse:
    """Classifica il testo di una segnalazione e restituisce la categoria più probabile."""
    if not nlp_classifier.is_loaded():
        raise HTTPException(status_code=503, detail="Modello non ancora caricato")
    try:
        return nlp_classifier.classify(request.testo)
    except Exception as e:
        logger.error("Errore durante la classificazione: %s", e)
        raise HTTPException(status_code=500, detail="Errore interno durante la classificazione")


@app.get("/health", response_model=HealthResponse)
async def health() -> HealthResponse:
    """Verifica lo stato del servizio e se il modello è disponibile."""
    return HealthResponse(
        status="ok",
        model_loaded=nlp_classifier.is_loaded(),
        model_name=MODEL_NAME,
    )


if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=PORT, reload=False)
