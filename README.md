
# 🗺️ OptiTour – Sistema di Ottimizzazione di Itinerari Turistici
<p align="center">
<img src="logoApp.PNG" alt="Logo OptiTour" width="300"/>
</p>
Pianificare un itinerario in una nuova città o in una meta turistica ricca di attrazioni può spesso trasformarsi in un vero e proprio rompicapo. C'è il rischio concreto di passare più tempo a incrociare mappe e orari che a godersi il viaggio, rendendo l'organizzazione manuale non solo stressante, ma spesso altamente inefficace. 

È proprio per rispondere a questa esigenza che nasce **OptiTour**. Più che un semplice pianificatore, è un sistema intelligente progettato per rivoluzionare l'organizzazione dei tuoi viaggi. L’applicazione prende in carico la tua "lista dei desideri" cioè i punti di interesse che vuoi assolutamente visitare e la trasforma in un percorso fluido e ottimizzato. L'obiettivo è farti risparmiare tempo e chilometri, riducendo al minimo la distanza complessiva e i tempi di percorrenza, pur rispettando vincoli realistici come il tempo necessario per visitare ogni singola tappa e l'effettiva conformazione geografica del territorio.


## Funzionalità Principali
* **Pianificazione e Ottimizzazione:** Calcola il percorso migliore tra i luoghi scelti, ottimizzando l'ordine delle tappe per risparmiare tempo e chilometri.
* **Itinerari su Misura:** Crea automaticamente un viaggio in base al tempo che hai a disposizione. Il sistema seleziona i monumenti assicurandosi che tu possa visitarli tutti e tornare al punto di partenza entro il limite orario impostato.
* **Gestione Itinerari:** Operazioni complete di creazione, modifica e cancellazione dei propri viaggi. È possibile aggiungere o rimuovere tappe in qualsiasi momento, ricalcolando il percorso in tempo reale.
* **Gestione Profilo:** Area personale dedicata alla modifica dei dati dell'utente, inclusa la gestione delle informazioni anagrafiche e delle credenziali.
* **Sicurezza:** Accesso protetto tramite autenticazione basata su standard moderni (JWT) e gestione sicura delle password per garantire la massima protezione della privacy.
* **Catalogo e Social:** Possibilità di sfogliare i viaggi condivisi dagli altri utenti, salvare i preferiti e pubblicare i propri percorsi migliori.
---

## Architettura del Sistema

Il progetto segue fedelmente il pattern architetturale **Model-View-Controller (MVC)**:

* **Model:** MongoDB (NoSQL) per una gestione flessibile e performante di POI, utenti e viaggi.
* **View:** React.js arricchito da React-Leaflet per una visualizzazione chiara e interattiva delle mappe.
* **Controller:** API REST sviluppate in Spring Boot per la gestione robusta della logica applicativa e delle richieste di rete.

---

## Stack Tecnologico

* **Backend:** Java + Spring Boot
* **Frontend:** React.js + Node.js
* **Database:** MongoDB
* **Routing:** GraphHopper
* **Geodati:** OpenStreetMap + Overpass API + Nominatim
* **API Client:** Axios

### Integrazione con Servizi Esterni
* **OpenStreetMap:** Per la base dei dati geografici.
* **Overpass API:** Per il recupero mirato dei POI.
* **Nominatim:** Per le operazioni di geocoding (conversione di indirizzi in coordinate).
* **GraphHopper:** Per il calcolo preciso di distanze e tempi di percorrenza reali.

---

## Casi d’Uso Principali

1.  **Creazione di un itinerario**: 
    * **UC12.1**: Creazione itinerario manuale.
    * **UC12.2**: Creazione itinerario casuale.
2.  **Ottimizzazione automatica delle tappe del percorso**: 
    * **UC14**: Ottimizzazione percorso (tramite algoritmi TSP).
3.  **Gestione e modifica successiva dei viaggi pianificati**:
    * **UC15**: Modifica tappe (aggiunta o rimozione di monumenti).
    * **UC16**: Elimina viaggio.
4.  **Visualizzazione del catalogo pubblico e del proprio storico viaggi**:
    * **UC3.1**: Visualizza catalogo (percorsi pubblici di altri utenti).
    * **UC3.3**: Visualizza storico (viaggi completati).
5.  **Condivisione e salvataggio dei percorsi preferiti**:
    * **UC8**: Pubblica percorso nel catalogo (condivisione).
    * **UC5**: Salva percorso nei preferiti.

---

## 💻 Installazione e Setup

### 1. Clona il repository
```bash
git clone https://github.com/TUO_USERNAME/OptiTour.git
cd OptiTour
```

### 2. Avvia MongoDB
Assicurati che l'istanza di MongoDB sia in esecuzione locale sulla porta di default:
* `localhost:27017`

### 3. (Opzionale) Configura GraphHopper
OptiTour usa GraphHopper per calcolare le distanze percorrendo le strade reali. **Se non viene configurato, l'applicazione funziona comunque** usando la formula di Haversine (distanza in linea d'aria) come fallback automatico — i percorsi saranno meno precisi ma tutte le funzionalità resteranno disponibili.
 
Per abilitare GraphHopper:
 
1. Crea la cartella `osm/` nella root del progetto
2. Scarica il file OSM della zona che ti interessa da [Geofabrik](https://download.geofabrik.de/europe/italy.html):
   - **Nord Italia** (Milano, Torino…) → `nord-ovest` 
   - **Italia intera** → `italy-latest.osm.pbf`
3. Rinomina il file scaricato in `map.osm.pbf` e mettilo nella cartella `osm/`
```
OptiTour/
├── osm/
│   └── map.osm.pbf   ← qui
├── src/
└── pom.xml
```
 
> Al primo avvio GraphHopper costruisce il grafo stradale e lo salva in `graphhopper-cache/` (può richiedere qualche minuto). Gli avvii successivi saranno immediati.

### 4. Avvia il Backend
Esegui il file principale dell'applicazione Java:
* `BackendApplication.java`
* *Il backend sarà disponibile all'indirizzo:* `http://localhost:8080`

### 5. Avvia il Frontend
Apri un nuovo terminale, spostati nella cartella del frontend e avvia il server di sviluppo:
```bash
cd src/main/frontend
npm install
npm run dev
```

### 6. Apri l’applicazione
Naviga sul tuo browser all'indirizzo:
* `http://localhost:5173`
