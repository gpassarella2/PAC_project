# 🗺️ BuddyMaps – Sistema di Ottimizzazione Viaggi Intelligente
![Logo BuddyMaps](logoApp.PNG)
BuddyMaps è un'applicazione avanzata di ottimizzazione logistica progettata per risolvere il problema decisionale che ogni turista affronta: scegliere cosa visitare è facile, ma decidere come farlo nel minor tempo possibile è una sfida complessa. A differenza delle tradizionali app di mappatura, BuddyMaps trasforma una lista disordinata di punti di interesse in un itinerario perfetto.
Il sistema offre all'utente la massima flessibilità: è possibile selezionare manualmente i singoli monumenti, attingere dai propri itinerari preferiti o lasciarsi ispirare dai percorsi consigliati. Inoltre, è possibile caricare itinerari preimpostati già ottimizzati. Una volta definiti i punti di interesse, l'app calcola automaticamente il percorso più breve per visitarli tutti in modo efficiente, ottimizzando gli spostamenti.

##Il Problema: Efficienza Urbana
Spesso i turisti perdono tempo prezioso camminando avanti e indietro per la città a causa di una pianificazione non ottimale. In informatica, questo è noto come Traveling Salesman Problem (TSP), un problema che diventa difficile da risolvere mentalmente all'aumentare dei luoghi da visitare.
BuddyMaps automatizza questo processo complesso utilizzando algoritmi avanzati sui Grafi ed euristiche dedicate. Il sistema non offre solo un percorso, ma il miglior percorso possibile, rendendolo lo strumento ideale per il "Turista Efficiente", il "Runner Urbano" o i professionisti dell'ospitalità.

##Funzionalità Principali
* **Pianificazione Itinerari Smart**: Selezione dei punti di interesse (POI) e calcolo del ciclo ottimale per ridurre chilometri e tempi di percorrenza.
* **Gestione Flessibile**: Possibilità di creare nuovi viaggi (manuali o casuali), modificare le tappe, visualizzare dettagli o eliminare percorsi.
* **Ottimizzazione TSP**: Modulo integrato che si interfaccia con provider geografici esterni per il calcolo del percorso ottimo.
* **Social e Storico**: Salvataggio dei percorsi preferiti, consultazione dello storico dei viaggi effettuati, condivisione e aggiunta di recensioni.
* **Gestione Profilo**: Registrazione, login e gestione sicura dei dati personali e delle credenziali.

##Architettura del Sistema
Il progetto segue il pattern architetturale Model-View-Controller (MVC) per separare nettamente la logica di presentazione, la logica di ottimizzazione e la gestione dei dati.

###Stack Tecnologico
* **Frontend (View)**: Sviluppato con React.js (componenti funzionali e JSX) e gestito tramite Node.js.
* **Backend (Controller)**: Implementato in Java con il framework Spring Boot.
* **Database (Model)**: Utilizzo di MongoDB (NoSQL) per gestire con flessibilità i POI e i dati geografici.
* **Integrazione Geografica**:
    * **OpenStreetMap**: Provider per coordinate e metadati dei monumenti.
    * **GraphHopper**: Motore di routing per il calcolo delle distanze e dei tempi reali.
* **Comunicazione**: Scambio dati tra client e server esclusivamente in formato JSON tramite Axios.

##Casi d'Uso
Il sistema risponde a diverse esigenze attraverso casi d'uso mirati:
* **UC20 / UC22**: Creazione di nuovi viaggi personalizzati o percorsi completamente nuovi.
* **UC25 / UC28**: Selezione dei monumenti e ottimizzazione automatica dell'ordine delle tappe.
* **UC15 / UC17**: Esportazione e condivisione degli itinerari con altri utenti.
