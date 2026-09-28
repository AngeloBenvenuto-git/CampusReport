# Modelli Pydantic per request/response del microservizio NLP

from pydantic import BaseModel, Field
from typing import List


class ClassifyRequest(BaseModel):
    testo: str = Field(..., min_length=3, max_length=2000)


class AlternativaCategoria(BaseModel):
    categoria: str
    score: float


class ClassifyResponse(BaseModel):
    categoria: str
    confidenza: float
    alternative: List[AlternativaCategoria]


class UrgenzaRequest(BaseModel):
    testo: str = Field(..., min_length=3, max_length=2000)


class UrgenzaResponse(BaseModel):
    urgenza: int = Field(..., ge=1, le=5)  # 1 = minima, 5 = massima
    confidenza: float
    descrizione: str  # descrizione del livello vincente


class HealthResponse(BaseModel):
    status: str
    model_loaded: bool
    model_name: str
