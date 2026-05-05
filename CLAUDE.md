# CampusReport — Sistema segnalazioni campus Unical

## Stack tecnico
- Backend: Spring Boot (Java), JWT auth, JPA/Hibernate, PostgreSQL
- Frontend: Angular + Leaflet.js
- NLP: Microservizio Python (FastAPI + HuggingFace Transformers)
- Email: JavaMailSender (SMTP)
- Build: Maven (backend), npm (frontend), pip (nlp-service)

## Struttura cartelle
- /backend — Spring Boot app (package root: it.unical.campusreport)
- /frontend — Angular app
- /nlp-service — Microservizio Python
- /docs — SRS e stato dell'arte

## Ruoli utente
- STUDENTE (email @studenti.unical.it) — priorità NORMALE
- DOCENTE (email @unical.it) — priorità ALTA
- TECNICO (creato dall'admin con invito email)
- ADMIN (account unico pre-configurato)

## Modello dati principale
- User (id, nome, cognome, email, password_hash, ruolo, attivo)
- Tecnico estende User (specializzazioni, zona, carico_massimo=10)
- Ticket (id, titolo, descrizione, categoria, stato, priorita, zona_id, segnalante_id, tecnico_id, categoria_confidenza, version, created_at, updated_at)
- CambioStato (id, ticket_id, stato_precedente, stato_nuovo, utente_id, nota, timestamp)
- Zona (id, nome, descrizione, geojson, colore)
- Allegato (id, ticket_id, filename, path, mimetype, dimensione, created_at)
- PasswordResetToken (id, user_id, token, scadenza, usato)

## Algoritmo assegnazione
score = α·match_specializzazione + β·(1−carico_attuale)
- match_specializzazione: 1 se categoria ticket è nelle specializzazioni del tecnico, 0 altrimenti
- carico_attuale: ticket_aperti / carico_massimo
- Pesi α e β configurabili dall'admin (devono sommare a 1)
- Carico massimo default: 10
- Se nessun tecnico disponibile: stato → IN_ATTESA, notifica admin

## Stati segnalazione
APERTA → ASSEGNATA → IN_LAVORAZIONE → COMPLETATA
Alternative: IN_ATTESA, RIFIUTATA

## Categorie
ELETTRICO, WIFI, IDRAULICO, ATTREZZATURA, ALTRO

## NLP — classificatore categorie
- Microservizio Python separato su porta 8000
- Endpoint: POST /classify { "testo": "..." } → { "categoria": "WIFI", "confidenza": 0.87 }
- Modello: zero-shot con mDeBERTa (HuggingFace)
- Il backend Spring chiama questo endpoint ad ogni nuova segnalazione

## Gestione concorrenza
- @Transactional + @Lock(PESSIMISTIC_WRITE) sull'assegnazione tecnico
- @Version (Optimistic Locking) sull'entità Ticket per cambi di stato

## Regole importanti
- Password sempre hashate con bcrypt cost factor >= 12
- Profilo dev: validazione dominio email disabilitata, Mailhog su porta 1025
- Profilo prod: domini @studenti.unical.it e @unical.it
- Tecnici NON si autoregistrano — solo l'admin crea i loro account
- Admin NON è registrabile dall'interfaccia pubblica
- Allegati: max 3 per ticket, max 5MB ciascuno, salvati in /uploads sul filesystem
- Filename allegati: UUID_timestamp_nomeoriginale (per evitare collisioni)
- Eseguire sempre i test prima di considerare un task completo

## Descrizione completa del progetto

CampusReport è un'applicazione web per la gestione delle segnalazioni di problemi tecnici all'interno del campus dell'Università della Calabria (Unical). Il sistema digitalizza e automatizza l'intero ciclo di vita di una segnalazione, dalla creazione da parte dell'utente fino alla risoluzione da parte del tecnico competente.

### Problema che risolve
Attualmente le segnalazioni di problemi tecnici (prese elettriche rotte, WiFi assente, attrezzature danneggiate, problemi idraulici) vengono gestite tramite canali informali come email, telefonate e messaggi WhatsApp. Questo approccio non offre tracciabilità, non permette di monitorare i tempi di risoluzione e non consente di raccogliere dati aggregati per la manutenzione preventiva.

### Attori del sistema

**Studente** — si registra con email @studenti.unical.it. Può inviare segnalazioni cliccando direttamente sulla mappa planimetrica del campus, monitorare lo stato delle proprie segnalazioni e ricevere notifiche email ad ogni cambio di stato. Le sue segnalazioni hanno priorità NORMALE.

**Docente** — si registra con email @unical.it. Ha le stesse funzionalità dello studente ma le sue segnalazioni hanno priorità ALTA e vengono elaborate prima nella coda dei tecnici.

**Tecnico** — non si registra autonomamente. L'admin crea il suo account specificando nome, email, specializzazioni (una o più categorie di competenza) e zona del campus. Il sistema invia una email con link di attivazione (validità 48 ore) per impostare la password. Il tecnico ha una dashboard dedicata con le segnalazioni assegnate ordinate per priorità, può aggiornare lo stato delle segnalazioni, aggiungere note interne e visualizzare le segnalazioni sulla mappa planimetrica.

**Admin** — account unico pre-configurato in fase di deployment. Gestisce gli account tecnici, configura i pesi dell'algoritmo di assegnazione (α e β) e il carico massimo per tecnico, può intervenire manualmente su qualsiasi segnalazione, visualizza statistiche globali e può esportare report CSV.

### Flusso principale di una segnalazione

1. L'utente autenticato apre la mappa interattiva del campus e clicca sulla zona dove si trova il problema
2. Si apre un form modale dove l'utente inserisce titolo, descrizione testuale e fino a 3 foto (max 5MB ciascuna). Mentre l'utente scrive la descrizione, il sistema chiama il microservizio NLP che suggerisce automaticamente la categoria più probabile. L'utente può accettare il suggerimento o cambiarlo manualmente da un menu a tendina (ELETTRICO, WIFI, IDRAULICO, ATTREZZATURA, ALTRO)
3. L'utente invia la segnalazione. Il sistema la salva con stato APERTA
4. Il backend chiama il microservizio NLP per confermare la categoria, poi calcola lo score per ogni tecnico disponibile con la formula: score = α·match_specializzazione + β·(1−carico_attuale). Il tecnico con score massimo viene assegnato. Lo stato passa ad ASSEGNATA. Il tecnico riceve notifica email
5. Il tecnico apre la dashboard, prende in carico la segnalazione: stato → IN_LAVORAZIONE. L'utente riceve notifica email
6. Il tecnico risolve il problema e aggiorna lo stato a COMPLETATA. L'utente riceve notifica email con riepilogo

**Flusso alternativo — nessun tecnico disponibile:** se nessun tecnico ha la specializzazione richiesta o tutti hanno raggiunto il carico massimo, la segnalazione va in stato IN_ATTESA e l'admin riceve notifica.

**Flusso alternativo — rifiuto:** il tecnico può rifiutare con motivazione obbligatoria. Il sistema ricalcola lo score escludendo il tecnico che ha rifiutato e assegna al successivo. Se non ci sono altri tecnici disponibili, stato → IN_ATTESA con notifica admin.

### Mappa interattiva
La mappa usa Leaflet.js con l'immagine planimetrica ufficiale del campus Unical come sfondo (L.imageOverlay). Sopra l'immagine sono definiti poligoni GeoJSON cliccabili corrispondenti alle macro-aree del campus (es. Polo Ingegneria Cubi 37-46, Biblioteca Cubo 25B, Polo Scientifico Cubi 7-18, ecc.). Al click su una zona si apre il form di segnalazione con la zona preimpostata. I marker mostrano le segnalazioni con colori diversi per stato. Gli studenti/docenti vedono solo le proprie segnalazioni, i tecnici vedono quelle assegnate a loro, l'admin vede tutto con filtri per categoria/stato/tecnico.

### Algoritmo di assegnazione automatica
Il cuore del sistema. Per ogni nuova segnalazione:
- Filtra i tecnici con match_specializzazione = 1 (specializzazione include la categoria della segnalazione)
- Per ogni tecnico calcola: score = α·match_specializzazione + β·(1−carico_attuale)
- carico_attuale = ticket_aperti / carico_massimo (default 10)
- Assegna il tecnico con score massimo
- I pesi α e β sono configurabili dall'admin e devono sommare a 1
- Usa @Transactional con @Lock(PESSIMISTIC_WRITE) per evitare race condition
- L'entità Ticket ha @Version per l'optimistic locking sui cambi di stato

### Classificatore NLP
Microservizio Python separato (FastAPI, porta 8000). Riceve la descrizione testuale della segnalazione e risponde con la categoria più probabile e il punteggio di confidenza. Usa un modello zero-shot pre-addestrato (MoritzLaurer/mDeBERTa-v3-base-mnli-xnli) che supporta l'italiano senza necessità di training. Il backend Spring lo chiama tramite RestTemplate o WebClient. Il classificatore viene invocato sia in tempo reale durante la compilazione del form (per il suggerimento) sia al momento del salvataggio definitivo (per confermare la categoria).

### Architettura tecnica
- Backend: Spring Boot 3.2.3, Java 17, Maven. Espone API REST JSON protette da JWT. Spring Security gestisce autenticazione e autorizzazione per ruolo. JPA/Hibernate gestisce la persistenza su PostgreSQL.
- Frontend: Angular 17+, TypeScript, Leaflet.js per la mappa. Comunica col backend tramite HTTP con JWT nell'header Authorization.
- NLP Service: Python 3.10+, FastAPI, HuggingFace Transformers. Microservizio indipendente.
- Database: PostgreSQL 15. Schema con tabelle: users, tickets, cambio_stato, zone, allegati, password_reset_tokens.
- Email: JavaMailSender con Mailhog in sviluppo e SMTP reale in produzione.
- Containerizzazione: Docker Compose con servizi postgres, mailhog, backend, nlp-service.

### Gestione ruoli e sicurezza
- JWT con scadenza 24 ore, refresh token
- Password hashate con bcrypt cost factor 12
- Ogni endpoint verifica il ruolo con @PreAuthorize
- In sviluppo: qualsiasi dominio email accettato, Mailhog per le email
- In produzione: solo @studenti.unical.it (STUDENTE) e @unical.it (DOCENTE)
- Tecnici creati solo dall'admin tramite invito email
- Admin non registrabile dall'interfaccia pubblica

### Modello dati
- User: id (UUID), nome, cognome, email, password_hash, ruolo (enum), attivo, created_at
- Tecnico estende User: specializzazioni (array di categoria), zona, carico_massimo
- Ticket: id (UUID), titolo, descrizione, categoria (enum), stato (enum), priorita (enum: ALTA/NORMALE), zona_id (FK), segnalante_id (FK), tecnico_id (FK nullable), categoria_confidenza (float), version (per optimistic locking), created_at, updated_at
- CambioStato: id, ticket_id (FK), stato_precedente, stato_nuovo, utente_id (FK), nota, timestamp
- Zona: id, nome, descrizione, geojson (JSON), colore (hex)
- Allegato: id, ticket_id (FK), filename, path, mimetype, dimensione, created_at
- PasswordResetToken: id, user_id (FK), token, scadenza, usato (boolean)

## Protezione delle rotte e autorizzazioni

### Backend — Spring Security (@PreAuthorize)
Ogni endpoint è protetto per ruolo. Regole:

| Endpoint | STUDENTE | DOCENTE | TECNICO | ADMIN |
|---|---|---|---|---|
| POST /auth/login | ✓ | ✓ | ✓ | ✓ |
| POST /auth/register | ✓ | ✓ | ✗ | ✗ |
| GET /api/map/zone | ✓ | ✓ | ✓ | ✓ |
| POST /api/tickets | ✓ | ✓ | ✗ | ✓ |
| GET /api/tickets/miei | ✓ | ✓ | ✗ | ✓ |
| GET /api/tickets/assegnati | ✗ | ✗ | ✓ | ✓ |
| PATCH /api/tickets/:id/stato | ✗ | ✗ | ✓ | ✓ |
| POST /api/tickets/:id/rifiuta | ✗ | ✗ | ✓ | ✗ |
| GET /api/admin/dashboard | ✗ | ✗ | ✗ | ✓ |
| POST /api/admin/tecnici | ✗ | ✗ | ✗ | ✓ |
| PUT /api/admin/tecnici/:id | ✗ | ✗ | ✗ | ✓ |
| GET /api/admin/statistiche | ✗ | ✗ | ✗ | ✓ |
| GET /api/admin/tickets | ✗ | ✗ | ✗ | ✓ |
| PATCH /api/admin/tickets/:id | ✗ | ✗ | ✗ | ✓ |
| GET /api/admin/export/csv | ✗ | ✗ | ✗ | ✓ |
| POST /api/admin/config/pesi | ✗ | ✗ | ✗ | ✓ |

Regole aggiuntive backend:
- Tutti gli endpoint /api/** richiedono JWT valido nell'header Authorization: Bearer <token>
- Gli endpoint /auth/** sono pubblici (no JWT richiesto)
- Il tecnico può modificare solo i ticket assegnati a lui, non quelli degli altri tecnici
- Lo studente/docente può vedere solo i propri ticket, non quelli altrui
- Usare @PreAuthorize("hasRole('RUOLO')") su ogni metodo del controller
- Configurare Spring Security per restituire 401 se JWT assente/scaduto e 403 se ruolo non autorizzato

### Frontend — Angular Route Guards
Protezione delle rotte con AuthGuard e RoleGuard:

| Rotta Angular | Ruoli ammessi | Redirect se non autorizzato |
|---|---|---|
| /login | solo non autenticati | /map |
| /register | solo non autenticati | /map |
| /map | STUDENTE, DOCENTE, TECNICO, ADMIN | /login |
| /tickets/miei | STUDENTE, DOCENTE | /login |
| /tickets/:id | STUDENTE, DOCENTE (solo propri) | /login |
| /dashboard | TECNICO | /login |
| /dashboard/ticket/:id | TECNICO | /login |
| /admin | ADMIN | /login |
| /admin/tecnici | ADMIN | /login |
| /admin/statistiche | ADMIN | /login |

Regole aggiuntive frontend:
- AuthGuard: verifica che il JWT sia presente e non scaduto nel localStorage
- RoleGuard: verifica che il ruolo dell'utente corrisponda a quello richiesto dalla rotta
- Se JWT scaduto: cancella localStorage e reindirizza a /login
- Dopo login reindirizza in base al ruolo: STUDENTE/DOCENTE → /map, TECNICO → /dashboard, ADMIN → /admin
- HttpInterceptor aggiunge automaticamente il JWT header a tutte le chiamate API
- Se il backend risponde 401, l'interceptor fa logout automatico e reindirizza a /login

## Ordine di sviluppo
1. Entità JPA e repository (backend)
2. Autenticazione JWT e Spring Security (backend)
3. API REST base per tickets e utenti (backend)
4. Microservizio NLP (nlp-service)
5. Algoritmo di assegnazione (backend)
6. Notifiche email (backend)
7. Mappa interattiva (frontend)
8. Dashboard tecnico (frontend)
9. Pannello admin (frontend)
10. Testing e integrazione finale

## Convenzioni di codice
- Java: camelCase per metodi e variabili, PascalCase per classi
- Tutti i DTO hanno suffisso Request (input) o Response (output)
- Tutti i service hanno interfaccia + implementazione
- Eccezioni custom nel package exception/ con suffisso Exception
- Angular: componenti in kebab-case, servizi con suffisso Service
- Tutti i metodi pubblici hanno Javadoc/JSDoc

## Porte in sviluppo locale
- Backend Spring Boot: http://localhost:8080
- Frontend Angular: http://localhost:4200
- NLP Service Python: http://localhost:8000
- PostgreSQL: localhost:5432 (db: campusreport_db, user: campusreport, password: campusreport)
- Mailhog UI: http://localhost:8025
- Mailhog SMTP: localhost:1025
