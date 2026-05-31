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


class HealthResponse(BaseModel):
    status: str
    model_loaded: bool
    model_name: str
