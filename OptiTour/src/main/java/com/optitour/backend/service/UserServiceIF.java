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

    UserProfileResponse getProfileByEmail(String email);

    UserProfileResponse updateProfile(String username, String firstName, String lastName);

    /**
     * Aggiorna le credenziali modificabili dell'utente (username, email).
     * Se newUsername o newEmail sono null/blank vengono ignorati.
     *
     * @param currentUsername username corrente (usato per identificare l'utente)
     * @param newUsername     nuovo username (opzionale)
     * @param newEmail        nuova email (opzionale)
     * @return profilo aggiornato
     * @throws IllegalArgumentException se il nuovo username o email sono già  in uso
     */
    UserProfileResponse updateCredentials(String currentUsername, String newUsername, String newEmail);

    /**
     * Elimina definitivamente l'account dell'utente e tutti i suoi viaggi dal DB.
     *
     * @param username username dell'utente da eliminare
     * @throws java.util.NoSuchElementException se l'utente non esiste
     */
    void deleteUser(String username);
}
