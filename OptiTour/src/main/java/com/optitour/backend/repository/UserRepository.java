package com.optitour.backend.repository;

import com.optitour.backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data MongoDB repository for {@link User} documents.
 */

//MongoRepository<T, ID>:
//T = User  → the document type stored in the collection.
//ID = String → the type of the primary key (@Id).

@Repository
public interface UserRepository extends MongoRepository<User, String> {

	// Optional avoids null values and prevents NullPointerExceptions.
    Optional<User> findByUsername(String username); // return a user with this username, if exists

    Optional<User> findByEmail(String email); // return a user with this email, if exists
    
    // Returns true if a user with this username exists in the database.
    boolean existsByUsername(String username);
    
    // Returns true if a user with this email exists in the database.
    boolean existsByEmail(String email); 
}
