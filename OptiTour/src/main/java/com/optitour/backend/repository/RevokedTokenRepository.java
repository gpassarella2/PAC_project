package com.optitour.backend.repository;

import com.optitour.backend.model.RevokedToken ;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link RevokedToken} documents – JWT logout blacklist.
 */
@Repository
public interface RevokedTokenRepository extends MongoRepository<RevokedToken, String> {

    boolean existsByToken(String token);

    void deleteByUsername(String username);

}