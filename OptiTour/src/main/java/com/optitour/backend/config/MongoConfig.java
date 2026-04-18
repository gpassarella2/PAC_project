package com.optitour.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB configuration enabling:
 *  - Repository scanning under com.optitour.backend.repository
 *  - Auditing for @CreatedDate / @LastModifiedDate annotations
 */

@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = "com.optitour.backend.repository")
public class MongoConfig {
    // Spring Boot auto-configures the MongoClient from application.properties.
    // This class adds auditing support and explicit repository scanning.
}