# 🗺️ BuddyMaps – L'Ottimizzatore di Viaggi Intelligente

![Logo BuddyMaps](logoApp.png)

**BuddyMaps** non è una semplice mappa: è un sistema intelligente che risolve il problema logistico di ogni turista. Tu scegli *cosa* vedere, noi calcoliamo *come* vederlo nel minor tempo possibile.

L'applicazione permette all'utente di selezionare dai 5 ai 10 monumenti in una città e calcola automaticamente il **percorso più breve** per visitarli tutti e tornare al punto di partenza, trasformando una lista disordinata di luoghi nell'itinerario perfetto.

---

## 🚀 Il Problema: Perché BuddyMaps?

Ti è mai capitato di camminare avanti e indietro per una città perdendo tempo prezioso?
Matematicamente, questo è noto come **Traveling Salesman Problem (TSP)**, un problema complesso (NP-Hard) che diventa difficile da risolvere a mente man mano che i punti di interesse aumentano.

BuddyMaps automatizza questo processo utilizzando **algoritmi sui Grafi** per trovare il ciclo ottimale, risparmiando ai turisti chilometri inutili e ore di cammino.

---

## ✨ Caratteristiche Principali

✅ **Pianificazione Smart** – Inserisci i punti di interesse (POI) e ottieni subito l'itinerario ottimizzato.
✅ **Gestione Itinerario** – Crea nuovi viaggi, modifica le tappe o elimina itinerari passati.
✅ **Algoritmo TSP Integrato** – Sfrutta euristiche avanzate (es. Nearest Neighbor) per calcolare il ciclo Hamiltoniano minimo.
✅ **Integrazione Geografica** – Si interfaccia con Provider esterni (es. Google Maps/OpenStreetMap) per calcolare le distanze reali su strada.
✅ **Storico Viaggi** – Salva i tuoi percorsi preferiti per riutilizzarli o condividerli in futuro.
✅ **Interfaccia Intuitiva** – Un design pulito che guida l'utente dalla selezione dei monumenti alla navigazione.

---

## ⚙️ Il Cuore Tecnologico

Il progetto si basa su una solida struttura algoritmica:

* **Modello Dati:** Grafo completo pesato, dove i **Nodi** rappresentano i monumenti e gli **Archi** rappresentano le distanze/tempi di percorrenza.
* **Motore di Calcolo:** Implementazione di algoritmi di approssimazione per il *Traveling Salesman Problem* per garantire risposte rapide anche su dispositivi mobili.
* **Architettura:** Separazione netta tra logica di presentazione (Utente) e logica di business (Sistema di Ottimizzazione).

---

## 🎯 Casi d'Uso

* 🏛️ **Il Turista Efficiente** – Vuole visitare 10 musei in un solo giorno senza perdere tempo nei trasporti.
* 🏃 **Il Runner Urbano** – Vuole pianificare un percorso di allenamento che tocchi vari landmark della città e torni esattamente a casa (punto di partenza).
* 🏨 **L'Hotel Concierge** – Vuole stampare rapidamente un itinerario ottimizzato per i propri ospiti basato sui loro interessi.

---

### 🛠️ Tecnologie (Esempio - Da compilare con le tue)
* *Linguaggio:* Java / Python / C++ (Scegli il tuo)
* *API Mappe:* Google Maps API / Mapbox
* *Diagrammi:* UML 2.0
