package com.example.mcpserver.config;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.DatabaseClientFactory.DigestAuthContext;
import com.marklogic.client.DatabaseClientFactory.BasicAuthContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 🎸 Epic MarkLogic Configuration - 2112 Style! 🎸
 * Configures the MarkLogic DatabaseClient for connecting to MarkLogic Server
 */
@Configuration
public class MarkLogicConfig {

    private static final Logger logger = LoggerFactory.getLogger(MarkLogicConfig.class);

    @Value("${marklogic.host}")
    private String host;

    @Value("${marklogic.port}")
    private int port;

    @Value("${marklogic.username}")
    private String username;

    @Value("${marklogic.password}")
    private String password;

    @Value("${marklogic.database}")
    private String database;

    @Value("${marklogic.authentication}")
    private String authentication;

    /**
     * 🚀 Creates the Epic MarkLogic DatabaseClient Bean
     * Connects to MarkLogic Server with the configured settings
     */
    @Bean
    public DatabaseClient databaseClient() {
        try {
            logger.info("🎸 Configuring Epic MarkLogic DatabaseClient...");
            logger.info("   Host: {}", host);
            logger.info("   Port: {}", port);
            logger.info("   Database: {}", database);
            logger.info("   Username: {}", username);
            logger.info("   Authentication: {}", authentication);

            // Create the DatabaseClient using the newer API
            DatabaseClient client;

            if ("basic".equalsIgnoreCase(authentication)) {
                // Use Basic Authentication
                client = DatabaseClientFactory.newClient(
                        host, port, database,
                        new BasicAuthContext(username, password));
            } else {
                // Default to Digest Authentication
                client = DatabaseClientFactory.newClient(
                        host, port, database,
                        new DigestAuthContext(username, password));
            }

            logger.info("✅ Epic MarkLogic DatabaseClient successfully configured!");
            logger.info("🚀 Ready to rock with MarkLogic Server!");

            return client;

        } catch (Exception e) {
            logger.error("💥 Failed to create MarkLogic DatabaseClient: {}", e.getMessage(), e);
            logger.error("🔧 Check your MarkLogic configuration properties:");
            logger.error("   - marklogic.host={}", host);
            logger.error("   - marklogic.port={}", port);
            logger.error("   - marklogic.database={}", database);
            logger.error("   - marklogic.username={}", username);
            logger.error("   - marklogic.authentication={}", authentication);

            // Re-throw the exception to fail fast
            throw new RuntimeException("Failed to configure MarkLogic DatabaseClient", e);
        }
    }
}
