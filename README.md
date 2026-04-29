# CampusReport

Sistema di segnalazione problemi e guasti per il campus dell'Università della Calabria (Unical).
Gli utenti autenticati possono aprire ticket georeferenziati sulla mappa del campus; i tecnici vengono assegnati automaticamente tramite un algoritmo di scoring basato su specializzazione e carico di lavoro.

## Stack tecnologico

| Layer      | Tecnologia                                              |
|------------|---------------------------------------------------------|
| Backend    | Spring Boot 3 (Java 17), JWT, JPA/Hibernate, PostgreSQL |
| Frontend   | Angular 17+, Leaflet.js                                 |
| NLP        | Python FastAPI, HuggingFace mDeBERTa (zero-shot)        |
| Email      | JavaMailSender — Mailhog in sviluppo                    |
| Build      | Maven (backend), npm (frontend), pip (nlp-service)      |

## Struttura cartelle

```
CampusReport/
├── backend/          # Spring Boot API (porta 8080)
├── frontend/         # Angular app (porta 4200 in dev)
├── nlp-service/      # Classificatore NLP (porta 8000)
├── docs/             # SRS e stato dell'arte
├── uploads/          # Allegati utenti — non in git
├── docker-compose.yml
├── CLAUDE.md
└── README.md
```

## Avvio con Docker

**Prerequisiti:** Docker Desktop installato e avviato.

```bash
# Prima esecuzione — costruisce le immagini
docker-compose up --build

# Avvii successivi
docker-compose up
```

| Servizio      | URL                      |
|---------------|--------------------------|
| Backend API   | http://localhost:8080    |
| NLP Service   | http://localhost:8000    |
| Mailhog UI    | http://localhost:8025    |
| PostgreSQL    | localhost:5432           |

Il frontend Angular va avviato separatamente con `ng serve` (vedi sotto).

## Sviluppo locale (senza Docker)

### Backend
```bash
cd backend
# Richiede PostgreSQL su localhost:5432 e Mailhog su localhost:1025
mvn spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
ng serve          # http://localhost:4200
```

### NLP Service
```bash
cd nlp-service
python -m venv .venv && source .venv/bin/activate  # o .venv\Scripts\activate su Windows
pip install -r requirements.txt
uvicorn main:app --reload
```

## Variabili d'ambiente (produzione)

In produzione sovrascrivere le variabili nel `docker-compose.yml` o tramite un file `.env` nella root:

| Variabile              | Descrizione                                  |
|------------------------|----------------------------------------------|
| `JWT_SECRET`           | Chiave HMAC-SHA256 (minimo 256 bit)          |
| `DB_URL`               | JDBC URL del database PostgreSQL             |
| `DB_USERNAME`          | Utente database                              |
| `DB_PASSWORD`          | Password database                            |
| `SMTP_HOST/PORT`       | Server email in produzione                   |
| `SMTP_USERNAME/PASSWORD` | Credenziali SMTP                           |
| `EMAIL_FROM`           | Indirizzo mittente delle email               |
| `CORS_ALLOWED_ORIGINS` | Origini consentite (es. https://campusreport.unical.it) |
