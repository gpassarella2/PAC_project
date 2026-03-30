package com.optitour.backend.service;

import com.optitour.backend.dto.UserRegisterRequest;
import com.optitour.backend.dto.UserProfileResponse;

/**
 * Interfaccia pubblica per le operazioni di business legate agli utenti.
 *
 * I controller dipendono da questa interfaccia, non dalla sua implementazione.
 *
 * Si occupa di:
 * - Registrazione di un nuovo utente
 * - Recupero del profilo pubblico
 * - Aggiornamento dei dati modificabili del profilo
 *
 * Note:
 * - Non espone entità interne (User)
 * - Non espone repository
 */
public interface UserServiceIF {

    /**
     * Registra un nuovo utente e restituisce il suo profilo pubblico.
     *
     * @param request DTO contenente i dati di registrazione
     * @return il profilo pubblico dell'utente appena creato
     * @throws IllegalArgumentException se username o email sono già utilizzati
     */
    UserProfileResponse register(UserRegisterRequest request);

    /**
     * Restituisce il profilo pubblico dell'utente con il dato username.
     *
     * @param username username univoco dell'utente
     * @return DTO del profilo pubblico
     * @throws java.util.NoSuchElementException se l'utente non esiste
     */
    UserProfileResponse getProfileByUsername(String username);

    /**
     * Restituisce il profilo pubblico dell'utente con la data email.
     *
     * @param email email univoca dell'utente
     * @return DTO del profilo pubblico
     * @throws java.util.NoSuchElementException se l'utente non esiste
     */
    UserProfileResponse getProfileByEmail(String email);

    /**
     * Aggiorna i campi modificabili del profilo utente.
     *
     * @param username username dell'utente da aggiornare
     * @param firstName nuovo nome (opzionale)
     * @param lastName nuovo cognome (opzionale)
     * @return DTO del profilo aggiornato
     * @throws java.util.NoSuchElementException se l'utente non esiste
     */
    UserProfileResponse updateProfile(String username, String firstName, String lastName);
}
