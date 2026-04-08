// ItineraryPage.jsx
// Pagina principale che mostra dati e mappa con punti di interesse di un itinerario turistico
// ─────────────────────────────────────────────────────────────────────────────

import React, { useEffect, useState, useRef } from 'react';

// per leggere i parametri URL e navigare tra le pagine
import { useParams, useNavigate } from 'react-router-dom';

// Componenti di react-leaflet per costruire la mappa interattiva
//  - MapContainer: contenitore principale della mappa
//  - TileLayer: layer dei "mattoncini" OpenStreetMap
//  - Marker: segnaposto su un punto della mappa
//  - Popup: finestra informativa che si apre al click su un Marker
//  - useMap: per accedere all'istanza della mappa dall'interno
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';

// Libreria Leaflet "pura" usata per creare icone personalizzate
// e per il plugin di routing
import L from 'leaflet';

// Componente Header condiviso dell'applicazione (barra di navigazione superiore)
import Header from '../components/Header';

// Funzioni di chiamata alle API del backend:
//  - getTripById:     GET /api/trips/{id}            → dati del viaggio
//  - getMonumentById: GET /api/monuments/{id}         → dettagli monumento
//  - optimizeTrip:    POST /api/trips/{id}/optimize   → ottimizzazione percorso
import { getTripById, getMonumentById, optimizeTrip } from '../services/api';


// Icone Leaflet personalizzate

/**
 * makeIcon
 * Crea un'icona circolare colorata con un numero/etichetta al centro.
 * Viene usata per i marker delle singole tappe (1, 2, 3…).
 *
 * @param {string} color  Colore di sfondo del cerchio
 * @param {string|number} label  Testo mostrato al centro
 * @returns {L.DivIcon} Icona Leaflet personalizzata 
 */
const makeIcon = (color, label) => L.divIcon({
  className: '',          // Nessuna classe CSS esterna (evita stili Leaflet di default)
  html: `<div style="
    background:${color};
    color:white;
    border-radius:50%;
    width:30px;height:30px;
    display:flex;align-items:center;justify-content:center;
    font-size:0.72rem;font-weight:700;
    border:2px solid white;
    box-shadow:0 1px 6px rgba(0,0,0,0.3);
    ">${label}</div>`,    
  iconSize: [30, 30],     
  iconAnchor: [15, 15],  
  popupAnchor: [0, -18],  
});

// Icona speciale per il punto di partenza (cerchio blu con "P")
// Ha dimensioni leggermente maggiori rispetto ai marker delle tappe
const startIcon = L.divIcon({
  className: '',
  html: `<div style="
    background:#2563eb;color:white;border-radius:50%;
    width:34px;height:34px;
    display:flex;align-items:center;justify-content:center;
    font-size:0.8rem;font-weight:700;
    border:2px solid white;box-shadow:0 1px 6px rgba(0,0,0,0.3);
    ">P</div>`,         
  iconSize: [34, 34],
  iconAnchor: [17, 17],
});


// ─── Formatting helpers ───────────────────────────────────────────────────────

/**
 * formatDuration
 * Converte una durata in secondi in una stringa leggibile (es. "1h 30min" o "45 min").
 *
 * @param {number} seconds - Durata in secondi
 * @returns {string} Stringa formattata
 */
function formatDuration(seconds) {
  const h = Math.floor(seconds / 3600);           // Calcola le ore intere
  const m = Math.floor((seconds % 3600) / 60);    // Calcola i minuti rimanenti
  if (h === 0) return `${m} min`;                 // Se meno di un'ora, mostra solo i minuti
  return `${h}h ${m}min`;                         // Altrimenti mostra ore e minuti
}

/**
 * formatDistance
 * Converte una distanza in metri in una stringa leggibile (es. "850 m" o "2.3 km").
 *
 * @param {number} meters - Distanza in metri
 * @returns {string} Stringa formattata
 */
function formatDistance(meters) {
  if (meters < 1000) return `${Math.round(meters)} m`;       // Sotto 1 km: mostra i metri
  return `${(meters / 1000).toFixed(1)} km`;                 // Sopra 1 km: mostra i chilometri con 1 decimale
}


// ─── Pagina Itinerario ────────────────────────────────────────────────────────

/**
 * ItineraryPage
 * Componente principale della pagina. Mostra:
 *  - Pannello laterale con nome viaggio, statistiche, lista tappe 
 *  - Mappa Leaflet con marker di partenza e marker delle tappe 
 */
export default function ItineraryPage() {
  // Legge il parametro :tripId dall'URL (es. /itinerary/42 → tripId = "42")
  const { tripId } = useParams();

  // Funzione per navigare programmaticamente (es. tornare alla pagina precedente)
  const navigate = useNavigate();

  // ── Stato locale ────────────────────────────────────────────────────────────
  const [trip, setTrip] = useState(null);               // Dati grezzi del viaggio dal backend
  const [enrichedStages, setEnrichedStages] = useState([]); // Tappe arricchite con nome, lat/lon, tipo, indirizzo
  const [loading, setLoading] = useState(true);         // True mentre il viaggio è in caricamento
  const [error, setError] = useState('');               // Messaggio di errore (se il fetch fallisce)
  const [optimizing, setOptimizing] = useState(false);  // True durante la chiamata di ottimizzazione
  const [optimizeError, setOptimizeError] = useState(''); // Messaggio di errore ottimizzazione


  // ── Effetto 1: Caricamento del viaggio ──────────────────────────────────────
  // Si esegue una sola volta (o quando cambia tripId).
  // Chiama GET /api/trips/{id} e salva il risultato in `trip`.
  useEffect(() => {
    getTripById(tripId)
      .then(res => setTrip(res.data))                          // Salva i dati del viaggio
      .catch(() => setError('Errore nel caricamento del viaggio')) // Mostra errore in caso di fallimento
      .finally(() => setLoading(false));                       // Rimuove il loading in ogni caso
  }, [tripId]); // Dipendenza: riesegue se cambia l'ID del viaggio nell'URL


  // ── Effetto 2: Arricchimento delle tappe ────────────────────────────────────
  // Si esegue ogni volta che `trip` cambia (cioè dopo il caricamento).
  // TripResponse.stages contiene solo { monumentId, visitDurationMinutes }.
  // Per ogni tappa fetcha i dettagli del monumento (nome, lat/lon, tipo, indirizzo)
  // tramite GET /api/monuments/{id}, poi unisce i dati.
  // Tutte le chiamate avvengono in parallelo con Promise.all per velocità.
  useEffect(() => {
    // Non fare nulla se il viaggio non è ancora caricato o non ha tappe
    if (!trip || !trip.stages || trip.stages.length === 0) return;

    Promise.all(
      trip.stages.map(async (stage) => {
        try {
          const res = await getMonumentById(stage.monumentId); // Fetch dettagli monumento
          // Unisce i dati del monumento con quelli della tappa;
          // visitDurationMinutes della tappa sovrascrive l'eventuale valore del monumento
          return { ...res.data, visitDurationMinutes: stage.visitDurationMinutes };
        } catch {
          // Se il fetch del monumento fallisce, usa i dati grezzi con il monumentId come nome
          return { ...stage, name: stage.monumentId };
        }
      })
    ).then(setEnrichedStages); // Salva tutte le tappe arricchite nell'array di stato
  }, [trip]); // Dipendenza: riesegue ogni volta che `trip` viene aggiornato


  // ── Handler: Ottimizzazione del percorso ────────────────────────────────────
  /**
   * handleOptimize
   * Chiamato al click su "Ottimizza percorso" o "Ri-ottimizza".
   * Chiama POST /api/trips/{id}/optimize e aggiorna trip ed enrichedStages
   * con i dati restituiti dal backend (già arricchiti con coordinate e metriche).
   */
  const handleOptimize = async () => {
    setOptimizing(true);       // Mostra il testo "Ottimizzazione in corso…"
    setOptimizeError('');      // Resetta eventuali errori precedenti

    try {
      const res = await optimizeTrip(tripId);   // Chiama l'API di ottimizzazione
      const optimized = res.data;               // Risposta con tappe ottimizzate e metriche

      // Aggiorna il trip: cambia lo stato a SAVED e aggiunge distanza e durata totale
      setTrip(prev => ({
        ...prev,                                                  // Mantieni tutti i campi esistenti
        status: 'SAVED',                                          // Segna il viaggio come ottimizzato
        totalDistanceMeters: optimized.totalDistanceMeters,       // Distanza totale del percorso
        totalDurationSeconds: optimized.totalDurationSeconds,     // Durata totale del percorso
      }));

      // Sostituisce le tappe con quelle ottimizzate (già complete di tutti i campi)
      setEnrichedStages(
        (optimized.stages || []).map(s => ({
          monumentId:           s.monumentId,           // ID del monumento
          name:                 s.name,                 // Nome del monumento
          type:                 s.type,                 // Tipo (es. "chiesa", "museo")
          lat:                  s.lat,                  // Latitudine
          lon:                  s.lon,                  // Longitudine
          address:              s.address,              // Indirizzo
          visitDurationMinutes: s.visitDurationMinutes, // Minuti di visita consigliati
        }))
      );
    } catch (e) {
      // In caso di errore mostra un messaggio sotto il bottone
      setOptimizeError('Errore durante l\'ottimizzazione. Riprova.');
    } finally {
      setOptimizing(false); // Riabilita il bottone in ogni caso
    }
  };


  // ── Render condizionale: loading ────────────────────────────────────────────
  // Mostra uno spinner finché i dati del viaggio non sono stati caricati
  if (loading) return (
    <div className="itin-page">
      <Header />
      <div className="loading-center"><div className="spinner" /><span>Caricamento itinerario...</span></div>
    </div>
  );

  // ── Render condizionale: errore ──────────────────────────────────────────────
  // Mostra un messaggio di errore se il caricamento del viaggio è fallito
  if (error) return (
    <div className="itin-page">
      <Header />
      <div className="page-content"><div className="error-msg">{error}</div></div>
    </div>
  );


  // ── Dati derivati per il render ──────────────────────────────────────────────

  // Alias locale: usiamo le tappe arricchite ([] durante il caricamento dei dettagli)
  const stages = enrichedStages;

  // Somma dei minuti di visita di tutte le tappe (per la statistica "Visita totale")
  const totalVisitMinutes = stages.reduce((acc, s) => acc + s.visitDurationMinutes, 0);

  // true se le tappe sono state arricchite e la prima ha le coordinate geografiche
  const hasCoords = stages.length > 0 && stages[0].lat !== undefined;

  // Centro della mappa: coordinate di partenza del viaggio (fallback: Roma)
  const mapCenter = [trip.startLat || 41.9, trip.startLon || 12.5];

  // Mappa dallo stato interno all'etichetta mostrata all'utente
  const statusLabel = { DRAFT: 'Bozza', SAVED: 'Ottimizzato', COMPLETED: 'Completato' };


  // ── Render principale ────────────────────────────────────────────────────────
  return (
    <div className="itin-page">
      <Header />

      {/* Layout a due colonne: pannello info a sinistra, mappa a destra */}
      <div className="itin-layout">

        {/* ─── Pannello informazioni (colonna sinistra) ─── */}
        <div className="itin-panel">

          {/* Bottone "Indietro": naviga alla pagina precedente nella cronologia */}
          <button
            className="btn btn-ghost btn-sm"
            style={{ marginBottom: 16 }}
            onClick={() => navigate(-1)}
          >
            Indietro
          </button>

          {/* Intestazione: nome del viaggio e badge della città */}
          <div className="itin-trip-header">
            <h1 className="page-title" style={{ fontSize: '1.3rem' }}>{trip.name}</h1>
            <span className="badge badge-muted">{trip.city}</span>
          </div>

          {/* ── Statistiche del viaggio ── */}
          <div className="itin-stats">
            {/* Numero totale di tappe (monumenti) */}
            <div className="itin-stat">
              <span className="itin-stat-label">Monumenti</span>
              <span className="itin-stat-val">{stages.length}</span>
            </div>

            {/* Tempo totale di visita (somma dei visitDurationMinutes di tutte le tappe) */}
            <div className="itin-stat">
              <span className="itin-stat-label">Visita totale</span>
              <span className="itin-stat-val">
                {Math.floor(totalVisitMinutes / 60)}h {totalVisitMinutes % 60}m
              </span>
            </div>

            {/* Distanza totale del percorso (visibile solo dopo l'ottimizzazione) */}
            {trip.totalDistanceMeters != null && (
              <div className="itin-stat">
                <span className="itin-stat-label">Distanza</span>
                <span className="itin-stat-val">{formatDistance(trip.totalDistanceMeters)}</span>
              </div>
            )}

            {/* Durata totale del percorso a piedi/in auto (visibile solo dopo l'ottimizzazione) */}
            {trip.totalDurationSeconds != null && (
              <div className="itin-stat">
                <span className="itin-stat-label">Percorso</span>
                <span className="itin-stat-val">{formatDuration(trip.totalDurationSeconds)}</span>
              </div>
            )}
          </div>

          {/* ── Punto di partenza ── (mostrato solo se il viaggio ha un startPoint) */}
          {trip.startPoint && (
            <div className="itin-start-point">
              <div>
                <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)', marginBottom: 2 }}>Punto di partenza</div>
                <div style={{ fontSize: '0.875rem', fontWeight: 600 }}>{trip.startPoint}</div>
              </div>
            </div>
          )}

          {/* ── Lista tappe in ordine di visita ── */}
          <h2 style={{ fontSize: '0.9rem', fontWeight: 600, margin: '18px 0 10px', color: 'var(--text-muted)' }}>
            ORDINE DI VISITA
          </h2>
          <div className="itin-stages">
            {/* Per ogni tappa arricchita, mostra una card con numero, nome, tipo, indirizzo e durata */}
            {stages.map((s, i) => (
              <div key={i} className="itin-stage-card">
                {/* Cerchio con il numero progressivo della tappa */}
                <div className="itin-stage-num">{i + 1}</div>

                <div className="itin-stage-info">
                  {/* Nome del monumento; fallback al monumentId se il nome non è disponibile */}
                  <div className="itin-stage-name">{s.name || s.monumentId}</div>
                  {/* Tipo di monumento (es. "museo", "chiesa") – mostrato solo se presente */}
                  {s.type && (
                    <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>{s.type}</span>
                  )}
                  {/* Indirizzo del monumento – mostrato solo se presente */}
                  {s.address && (
                    <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)' }}>{s.address}</div>
                  )}
                </div>

                {/* Durata consigliata della visita in minuti */}
                <div className="itin-stage-time">{s.visitDurationMinutes} min</div>
              </div>
            ))}
          </div>

          {/* ── Stato attuale del viaggio ── */}
          <div style={{ marginTop: 16, padding: '10px 14px', background: '#f0fdf4', borderRadius: 'var(--radius-sm)', border: '1px solid #bbf7d0' }}>
            <span style={{ color: 'var(--green)', fontSize: '0.825rem' }}>
              {/* Converte lo stato interno (DRAFT/SAVED/COMPLETED) in etichetta leggibile */}
              Stato: <strong>{statusLabel[trip.status] || trip.status}</strong>
            </span>
          </div>

          {/* ── Bottone "Ottimizza percorso" – visibile solo se il viaggio è in bozza ── */}
          {trip.status === 'DRAFT' && (
            <>
              <button
                className="btn btn-primary"
                style={{ marginTop: 12, width: '100%' }}
                onClick={handleOptimize}
                disabled={optimizing} // Disabilitato durante l'elaborazione
              >
                {/* Testo dinamico: cambia durante l'elaborazione */}
                {optimizing ? 'Ottimizzazione in corso…' : '✦ Ottimizza percorso'}
              </button>
              {/* Messaggio di errore ottimizzazione (visibile solo in caso di fallimento) */}
              {optimizeError && (
                <div style={{ color: 'red', fontSize: '0.78rem', marginTop: 6 }}>
                  {optimizeError}
                </div>
              )}
            </>
          )}

          {/* ── Bottone "Ri-ottimizza" – visibile solo se il viaggio è già stato ottimizzato ── */}
          {trip.status === 'SAVED' && (
            <button
              className="btn btn-ghost btn-sm"
              style={{ marginTop: 10, width: '100%' }}
              onClick={handleOptimize}
              disabled={optimizing} // Disabilitato durante il ricalcolo
            >
              {optimizing ? 'Ricalcolo…' : '↺ Ri-ottimizza'}
            </button>
          )}

        </div>{/* fine itin-panel */}


        {/* ─── Mappa interattiva (colonna destra) ─── */}
        <div className="itin-map-wrapper">
          <MapContainer
            center={mapCenter}          // Coordinate di centro della mappa all'apertura
            zoom={13}                   // Zoom iniziale (13 = livello città)
            style={{ width: '100%', height: '100%' }}
            scrollWheelZoom={true}      // Abilita lo zoom con la rotellina del mouse
          >
            {/* Layer dei tile OpenStreetMap (la "carta geografica" di sfondo) */}
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />

            {/* Marker del punto di partenza (icona "P" blu) – visibile solo se le coordinate non sono (0,0) */}
            {trip.startLat !== 0 && (
              <Marker position={[trip.startLat, trip.startLon]} icon={startIcon}>
                <Popup><strong>Partenza</strong><br />{trip.startPoint}</Popup>
              </Marker>
            )}

            {/* Marker per ogni tappa (numerati 1, 2, 3…) – visibili solo se le coordinate sono disponibili */}
            {hasCoords && stages.map((s, i) => (
              <Marker key={i} position={[s.lat, s.lon]} icon={makeIcon('#2563eb', i + 1)}>
                <Popup>
                  <strong>{s.name || s.monumentId}</strong><br />
                  {s.visitDurationMinutes} min<br />
                  {s.address && <span>{s.address}</span>}
                </Popup>
              </Marker>
            ))}

          </MapContainer>
        </div>
      </div>

      {/* ─── Stili CSS della pagina ─── */}
      <style>{`
        /* Contenitore principale: occupa tutta l'altezza della viewport */
        .itin-page { min-height: 100vh; display: flex; flex-direction: column; background: var(--bg); }

        /* Layout a griglia: pannello fisso 380px a sinistra, mappa flessibile a destra */
        .itin-layout {
          flex: 1;
          display: grid;
          grid-template-columns: 380px 1fr;
          height: calc(100vh - 56px); /* Altezza totale meno l'header */
          overflow: hidden;
        }

        /* Pannello laterale: scorrevole verticalmente, sfondo bianco */
        .itin-panel {
          padding: 20px;
          overflow-y: auto;
          border-right: 1px solid var(--border);
          background: #fff;
        }

        /* Intestazione del viaggio: nome e badge città affiancati */
        .itin-trip-header {
          display: flex; align-items: flex-start;
          justify-content: space-between; gap: 12px;
          margin-bottom: 16px; flex-wrap: wrap;
        }

        /* Griglia 2 colonne per le statistiche */
        .itin-stats {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 8px;
          margin-bottom: 16px;
        }

        /* Singola card statistica */
        .itin-stat {
          display: flex; flex-direction: column; gap: 2px;
          background: var(--bg);
          border: 1px solid var(--border);
          border-radius: var(--radius-sm);
          padding: 10px 12px;
        }

        /* Etichetta della statistica (es. "Monumenti") */
        .itin-stat-label { font-size: 0.7rem; color: var(--text-dim); text-transform: uppercase; letter-spacing: 0.04em; }

        /* Valore della statistica (es. "5") */
        .itin-stat-val { font-size: 0.95rem; font-weight: 700; color: var(--text); }

        /* Box punto di partenza con sfondo azzurro tenue */
        .itin-start-point {
          display: flex; align-items: center; gap: 10px;
          background: var(--accent-soft);
          border: 1px solid #bfdbfe;
          border-radius: var(--radius-sm);
          padding: 10px 14px;
          margin-bottom: 8px;
        }

        /* Lista verticale delle tappe */
        .itin-stages { display: flex; flex-direction: column; gap: 6px; }

        /* Card di una singola tappa */
        .itin-stage-card {
          display: flex; align-items: flex-start; gap: 10px;
          background: var(--bg);
          border: 1px solid var(--border);
          border-radius: var(--radius-sm);
          padding: 10px 12px;
        }

        /* Cerchio numerato della tappa */
        .itin-stage-num {
          width: 24px; height: 24px;
          background: var(--accent);
          border-radius: 50%;
          display: flex; align-items: center; justify-content: center;
          font-size: 0.7rem; font-weight: 700; color: white;
          flex-shrink: 0; /* Non si restringe */
        }

        /* Colonna centrale con nome, tipo e indirizzo */
        .itin-stage-info { flex: 1; display: flex; flex-direction: column; gap: 2px; }

        /* Nome del monumento */
        .itin-stage-name { font-size: 0.875rem; font-weight: 600; }

        /* Durata della visita (allineata a destra, non va a capo) */
        .itin-stage-time { font-size: 0.78rem; color: var(--text-muted); white-space: nowrap; flex-shrink: 0; }

        /* Wrapper della mappa: riempie lo spazio disponibile */
        .itin-map-wrapper { position: relative; }

        /* Layout responsive per mobile: stack verticale, mappa alta 50vh */
        @media (max-width: 768px) {
          .itin-layout { grid-template-columns: 1fr; height: auto; }
          .itin-map-wrapper { height: 50vh; }
        }
      `}</style>
    </div>
  );
}
