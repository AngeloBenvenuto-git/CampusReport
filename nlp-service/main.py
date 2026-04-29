from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from transformers import pipeline
from dotenv import load_dotenv
import logging
import os

load_dotenv()

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

MODEL_NAME = os.getenv("MODEL_NAME", "MoritzLaurer/mDeBERTa-v3-base-mnli-xnli")
CATEGORIES = ["ELETTRICO", "WIFI", "IDRAULICO", "ATTREZZATURA", "ALTRO"]

classifier = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global classifier
    logger.info(f"Caricamento modello: {MODEL_NAME}")
    classifier = pipeline(
        "zero-shot-classification",
        model=MODEL_NAME,
        device=-1,  # CPU; impostare 0 per GPU
    )
    logger.info("Modello caricato con successo")
    yield
    classifier = None


app = FastAPI(
    title="CampusReport NLP Service",
    version="1.0.0",
    lifespan=lifespan,
)


class ClassifyRequest(BaseModel):
    testo: str


class Alternative(BaseModel):
    categoria: str
    confidenza: float


class ClassifyResponse(BaseModel):
    categoria: str
    confidenza: float
    alternative: list[Alternative]


@app.post("/classify", response_model=ClassifyResponse)
async def classify(request: ClassifyRequest) -> ClassifyResponse:
    if classifier is None:
        raise HTTPException(status_code=503, detail="Modello non ancora caricato")
    if not request.testo.strip():
        raise HTTPException(status_code=400, detail="Il testo non può essere vuoto")

    result = classifier(request.testo, CATEGORIES)
    labels: list[str] = result["labels"]
    scores: list[float] = result["scores"]

    alternative = [
        Alternative(categoria=label, confidenza=round(score, 4))
        for label, score in zip(labels[1:], scores[1:])
    ]

    return ClassifyResponse(
        categoria=labels[0],
        confidenza=round(scores[0], 4),
        alternative=alternative,
    )


@app.get("/health")
async def health() -> dict:
    return {
        "status": "ok",
        "model_loaded": classifier is not None,
        "model_name": MODEL_NAME,
    }
