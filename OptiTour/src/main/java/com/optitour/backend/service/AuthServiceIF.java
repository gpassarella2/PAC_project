package com.optitour.backend.service;

import com.optitour.backend.dto.ChangePasswordRequest;

/**
 * Interfaccia pubblica per le operazioni di business legate
 * all'autenticazione e alla gestione dei token JWT.
 * 
 * Si occupa di:
 * - Logout stateless tramite revoca del token JWT
 * - Cambio password con verifica della password attuale
 * - Controllo dello stato di revoca di un token
 *
 * Note:
 * - Non espone entità interne (User, RevokedToken)
 * - Non espone repository
 * - I controller dipendono da questa interfaccia, non dall'implementazione
 */
public interface AuthServiceIF {

    /**
     * Invalida il JWT fornito aggiungendolo alla blacklist dei token revocati.
     * Le richieste successive che utilizzano questo token verranno rifiutate
     * dal filtro di sicurezza.
     *
     * @param rawToken il token JWT in formato raw (senza prefisso "Bearer ")
     * @param username username dell'utente autenticato
     */
    void logout(String rawToken, String username);

    /**
     * Restituisce true se il token fornito risulta revocato
     * (ad esempio perché l'utente ha effettuato il logout).
     *
     * @param rawToken il token JWT in formato raw
     * @return true se il token è revocato, false altrimenti
     */
    boolean isTokenRevoked(String rawToken);

    /**
     * Cambia la password dell'utente dopo aver verificato quella attuale.
     *
     * @param username username dell'utente autenticato
     * @param request DTO contenente password attuale e nuova password
     * @throws IllegalArgumentException se la password attuale non è corretta
     */
    void changePassword(String username, ChangePasswordRequest request);
}
