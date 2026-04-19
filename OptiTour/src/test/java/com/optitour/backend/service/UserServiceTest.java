package com.optitour.backend.service;

import com.optitour.backend.dto.UserRegisterRequest;
import com.optitour.backend.dto.UserProfileResponse;
import com.optitour.backend.model.User;
import com.optitour.backend.repository.TripRepository;
import com.optitour.backend.repository.UserRepository;
import com.optitour.backend.service.impl.UserServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Abilita Mockito per i test JUnit 5
class UserServiceTest {

	@Mock
	private UserRepository userRepository; // Mock del repository MongoDB degli utenti

	@Mock
	private TripRepository tripRepository; // Mock del repository MongoDB dei trip

	@Mock
	private PasswordEncoder passwordEncoder; // Mock dell'encoder password

	@InjectMocks
	private UserServiceImpl userService; // Il service sotto test, con le dipendenze mockate

	// --- REGISTER -------------------------------------

	@Test
	void register_success() {
		// Input della registrazione
		UserRegisterRequest req = new UserRegisterRequest("gigi", "gigi@test.com", "Password123!", "Gigi", "Rossi");

		// Simula che username ed email non siano già usati
		when(userRepository.existsByUsername("gigi")).thenReturn(false);
		when(userRepository.existsByEmail("gigi@test.com")).thenReturn(false);

		// Simula l'encoding della password
		when(passwordEncoder.encode("Password123!")).thenReturn("hashed");

		// Utente che il repository restituirà dopo il salvataggio
		User saved = User.builder().id("123").username("gigi").email("gigi@test.com").password("hashed")
				.firstName("Gigi").lastName("Rossi").build();

		when(userRepository.save(any(User.class))).thenReturn(saved);

		// Esegue il metodo register da testare
		UserProfileResponse response = userService.register(req);

		// Verifica che il risultato sia corretto
		assertEquals("gigi", response.getUsername());
		assertEquals("gigi@test.com", response.getEmail());
		assertEquals("Gigi", response.getFirstName());
	}

	@Test
	void register_fails_whenUsernameExists() {
		// Simula che l'username esiste già
		UserRegisterRequest req = new UserRegisterRequest("gigi", "gigi@test.com", "Password123!", "Gigi", "Rossi");

		when(userRepository.existsByUsername("gigi")).thenReturn(true);

		// UserService deve lanciare l'eccezione
		assertThrows(IllegalArgumentException.class, () -> userService.register(req));
	}

	@Test
	void register_fails_whenEmailExists() {
		// Simula che l'email è già stata registrata
		UserRegisterRequest req = new UserRegisterRequest("gigi", "gigi@test.com", "Password123!", "Gigi", "Rossi");

		when(userRepository.existsByUsername("gigi")).thenReturn(false);
		when(userRepository.existsByEmail("gigi@test.com")).thenReturn(true);

		assertThrows(IllegalArgumentException.class, () -> userService.register(req));
	}

	// --- GET PROFILE BY USERNAME -------------------------------------

	@Test
	void getProfileByUsername_success() {
		// Utente trovato nel DB
		User user = User.builder().id("123").username("gigi").email("gigi@test.com").build();

		when(userRepository.findByUsername("gigi")).thenReturn(Optional.of(user));

		UserProfileResponse response = userService.getProfileByUsername("gigi");

		assertEquals("gigi", response.getUsername());
	}

	@Test
	void getProfileByUsername_notFound() {
		// Nessun utente trovato
		when(userRepository.findByUsername("gigi")).thenReturn(Optional.empty());

		assertThrows(NoSuchElementException.class, () -> userService.getProfileByUsername("gigi"));
	}

	// --- GET PROFILE BY EMAIL -------------------------------------

	@Test
	void getProfileByEmail_success() {
		User user = User.builder().id("123").username("gigi").email("gigi@test.com").build();

		when(userRepository.findByEmail("gigi@test.com")).thenReturn(Optional.of(user));

		UserProfileResponse response = userService.getProfileByEmail("gigi@test.com");

		assertEquals("gigi@test.com", response.getEmail());
	}

	// --- UPDATE PROFILE -------------------------------------

	@Test
	void updateProfile_success() {
		// Utente esistente
		User user = User.builder().id("123").username("gigi").email("gigi@test.com").firstName("Old").lastName("Name")
				.build();

		when(userRepository.findByUsername("gigi")).thenReturn(Optional.of(user));

		// Ritorna l'utente modificato
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

		UserProfileResponse response = userService.updateProfile("gigi", "New", "Surname");

		assertEquals("New", response.getFirstName());
		assertEquals("Surname", response.getLastName());
	}

	// --- UPDATE CREDENZIALI -------------------------------------

	/**
	 * Verifica che updateCredentials aggiorni correttamente lo username quando il
	 * nuovo valore è valido e non già utilizzato. L'email deve rimanere invariata.
	 */
	@Test
	void updateCredentials_changesUsername_success() {
		// Utente esistente con username "gigi"
		User user = User.builder().id("123").username("gigi").email("gigi@test.com").build();

		when(userRepository.findByUsername("gigi")).thenReturn(Optional.of(user));
		// Il nuovo username "gigi2" non è ancora in uso
		when(userRepository.existsByUsername("gigi2")).thenReturn(false);
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

		UserProfileResponse response = userService.updateCredentials("gigi", "gigi2", null);

		assertEquals("gigi2", response.getUsername());
		assertEquals("gigi@test.com", response.getEmail());
	}

	/**
	 * Verifica che updateCredentials aggiorni correttamente l'email quando il nuovo
	 * valore è disponibile e differente da quella attuale. Lo username deve
	 * rimanere invariato.
	 */
	@Test
	void updateCredentials_changesEmail_success() {
		User user = User.builder().id("123").username("gigi").email("gigi@test.com").build();

		when(userRepository.findByUsername("gigi")).thenReturn(Optional.of(user));
		// La nuova email non è già registrata
		when(userRepository.existsByEmail("nuovo@test.com")).thenReturn(false);
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

		UserProfileResponse response = userService.updateCredentials("gigi", null, "nuovo@test.com");

		assertEquals("gigi", response.getUsername());
		assertEquals("nuovo@test.com", response.getEmail());
	}

	/**
	 * Verifica che updateCredentials aggiorni sia username che email quando
	 * entrambi i nuovi valori sono validi e non già registrati.
	 */
	@Test
	void updateCredentials_changesBoth_success() {
		User user = User.builder().id("123").username("gigi").email("gigi@test.com").build();

		when(userRepository.findByUsername("gigi")).thenReturn(Optional.of(user));
		when(userRepository.existsByUsername("gigino")).thenReturn(false);
		when(userRepository.existsByEmail("gigino@test.com")).thenReturn(false);
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

		UserProfileResponse response = userService.updateCredentials("gigi", "gigino", "gigino@test.com");

		assertEquals("gigino", response.getUsername());
		assertEquals("gigino@test.com", response.getEmail());
	}

	/**
	 * Se username e email coincidono con i valori attuali, allora il metodo non
	 * deve effettuare controlli di unicità o apportare modifiche
	 */
	@Test
	void updateCredentials_sameUsernameAndEmail_noSaveNeeded() {
		// Se username e email non cambiano, il comportamento deve essere stabile
		User user = User.builder().id("123").username("gigi").email("gigi@test.com").build();

		when(userRepository.findByUsername("gigi")).thenReturn(Optional.of(user));
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

		// Passa gli stessi valori: non deve sollevare eccezioni
		UserProfileResponse response = userService.updateCredentials("gigi", "gigi", "gigi@test.com");

		assertEquals("gigi", response.getUsername());
		assertEquals("gigi@test.com", response.getEmail());
		// existsByUsername e existsByEmail NON devono essere chiamati per valori
		// identici
		verify(userRepository, never()).existsByUsername("gigi");
		verify(userRepository, never()).existsByEmail("gigi@test.com");
	}

	/**
	 * Verifica che venga sollevata un'eccezione quando il nuovo username risulta
	 * già utilizzato da un altro utente. Non deve effettuare nessun salvataggio
	 */
	@Test
	void updateCredentials_fails_whenNewUsernameAlreadyTaken() {
		User user = User.builder().id("123").username("gigi").email("gigi@test.com").build();

		when(userRepository.findByUsername("gigi")).thenReturn(Optional.of(user));
		// "mario" è già  in uso
		when(userRepository.existsByUsername("mario")).thenReturn(true);

		assertThrows(IllegalArgumentException.class, () -> userService.updateCredentials("gigi", "mario", null));

		// Il repository non deve salvare nulla
		verify(userRepository, never()).save(any());
	}

	/**
	 * Verifica che venga sollevata un'eccezione quando la nuova email risulta già
	 * registrata nel sistema. Non deve effettuare nessun salvataggio
	 */
	@Test
	void updateCredentials_fails_whenNewEmailAlreadyRegistered() {
		User user = User.builder().id("123").username("gigi").email("gigi@test.com").build();

		when(userRepository.findByUsername("gigi")).thenReturn(Optional.of(user));
		when(userRepository.existsByEmail("altro@test.com")).thenReturn(true);

		assertThrows(IllegalArgumentException.class,
				() -> userService.updateCredentials("gigi", null, "altro@test.com"));

		verify(userRepository, never()).save(any());
	}

	/**
	 * Verifica che updateCredentials sollevi NoSuchElementException quando l'utente
	 * indicato non esiste nel database.
	 */
	@Test
	void updateCredentials_fails_whenUserNotFound() {
		when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

		assertThrows(NoSuchElementException.class, () -> userService.updateCredentials("ghost", "newname", null));
	}

	/**
	 * Verifica che un nuovo username vuoto o composto solo da spazi venga ignorato
	 * e non sovrascriva quello esistente. In questo caso non devono essere eseguiti
	 * controlli di unicità.
	 */
	@Test
	void updateCredentials_ignoresBlankUsername() {
		// Un username blank non deve sovrascrivere quello esistente
		User user = User.builder().id("123").username("gigi").email("gigi@test.com").build();

		when(userRepository.findByUsername("gigi")).thenReturn(Optional.of(user));
		when(userRepository.existsByEmail("nuovo@test.com")).thenReturn(false);
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

		UserProfileResponse response = userService.updateCredentials("gigi", "  ", "nuovo@test.com");

		// Username invariato
		assertEquals("gigi", response.getUsername());
		assertEquals("nuovo@test.com", response.getEmail());
		verify(userRepository, never()).existsByUsername(any());
	}

	// ---- DELETE USER -------------------------------------

	/**
	 * Verifica che deleteUser elimini correttamente un utente esistente.
	 * Deve prima cancellare tutti i viaggi associati all'utente e poi
	 * rimuovere l'utente stesso dal database, senza sollevare eccezioni.
	 */
	@Test
	void deleteUser_success() {
		User user = User.builder().id("123").username("gigi").email("gigi@test.com").build();

		when(userRepository.findByUsername("gigi")).thenReturn(Optional.of(user));

		// Nessuna eccezione attesa
		assertDoesNotThrow(() -> userService.deleteUser("gigi"));

		// I viaggi dell'utente devono essere eliminati prima dell'account
		verify(tripRepository, times(1)).deleteByUserId("123");
		// L'utente deve essere rimosso dal DB
		verify(userRepository, times(1)).delete(user);
	}

	/**
	 * Verifica che deleteUser invochi deleteByUserId passando l'ID corretto
	 * dell'utente, assicurando che la cancellazione dei viaggi avvenga
	 * utilizzando il campo id del documento User e non lo username.
	 */
	@Test
	void deleteUser_alsoDeletesAllTrips() {
		// Verifica esplicita che deleteByUserId venga invocato con l'ID corretto
		User user = User.builder().id("abc-456").username("mario").email("mario@test.com").build();

		when(userRepository.findByUsername("mario")).thenReturn(Optional.of(user));

		userService.deleteUser("mario");

		// Deve passare l'ID del documento User, non lo username
		verify(tripRepository).deleteByUserId("abc-456");
	}

	/**
	 * Verifica che deleteUser sollevi NoSuchElementException quando
	 * l'utente richiesto non esiste.
	 * In questo caso non devono essere effettuate operazioni né sui viaggi né sul repository utente.
	 */
	@Test
	void deleteUser_fails_whenUserNotFound() {
		when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

		assertThrows(NoSuchElementException.class, () -> userService.deleteUser("ghost"));

		// Non deve toccare nÃ© i viaggi nÃ© il repository utente in caso di eccezione
		verify(tripRepository, never()).deleteByUserId(any());
		verify(userRepository, never()).delete(any(User.class));
	}

	/**
	 * Verifica che deleteUser termini correttamente anche quando
	 * l'utente non ha viaggi associati. Il metodo deleteByUserId non
	 * deve sollevare eccezioni e deve comunque eliminare l'utente.
	 */
	@Test
	void deleteUser_deletesUserEvenWithNoTrips() {
		// deleteByUserId su MongoDB non lancia eccezioni se non ci sono documenti da
		// eliminare:
		// questo test verifica che il flusso completi correttamente in tal caso
		User user = User.builder().id("999").username("vuoto").email("vuoto@test.com").build();

		when(userRepository.findByUsername("vuoto")).thenReturn(Optional.of(user));
		doNothing().when(tripRepository).deleteByUserId("999");

		assertDoesNotThrow(() -> userService.deleteUser("vuoto"));

		verify(tripRepository).deleteByUserId("999");
		verify(userRepository).delete(user);
	}

}
