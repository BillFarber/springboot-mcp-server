package com.example.mcpserver.config;

import org.mockito.Mockito;
import org.springframework.ai.chat.ChatClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration to provide mock ChatClient for unit tests
 */
@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean
    @Primary
    public ChatClient chatClient() {
        return Mockito.mock(ChatClient.class);
    }
}
