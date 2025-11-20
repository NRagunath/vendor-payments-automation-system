package com.shanthigear.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanthigear.VendorPaymentApplication;

@SpringBootTest(
    classes = VendorPaymentApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseIntegrationTest implements AutoCloseable {

    @Container
    protected static final OracleContainer oracleContainer;
    
    static {
        oracleContainer = new OracleContainer(DockerImageName.parse("gvenzl/oracle-xe:21-slim"))
            .withDatabaseName("XEPDB1")  // Default service name for Oracle XE PDB
            .withUsername("TEST_USER")
            .withPassword("testpass")
            .withEnv("ORACLE_PASSWORD", "oracle")  // SYS password
            .withStartupTimeout(Duration.ofMinutes(5))
            .withReuse(true);
        
        // Start the container immediately to ensure it's ready before any test runs
        oracleContainer.start();
    }
        
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @DynamicPropertySource
    static void oracleProperties(DynamicPropertyRegistry registry) {
        // Use the default connection URL but replace the SID with PDB service name
        String jdbcUrl = oracleContainer.getJdbcUrl()
            .replace(":xe", ":XEPDB1")
            .replace("xe", "XEPDB1");
            
        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.username", oracleContainer::getUsername);
        registry.add("spring.datasource.password", oracleContainer::getPassword);
        
        // Oracle specific properties
        registry.add("spring.datasource.driver-class-name", () -> "oracle.jdbc.OracleDriver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.Oracle12cDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.sql.init.mode", () -> "always");
        
        // Additional Oracle properties
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.Oracle12cDialect");
        registry.add("spring.jpa.properties.hibernate.id.new_generator_mappings", () -> "false");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "20000");
        registry.add("spring.datasource.hikari.minimum-idle", () -> "5");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "10");
    }

    @BeforeEach
    void setUp() {
        // Common setup for all integration tests
    }
    
    @Override
    public void close() {
        // No need to close the container here as it's managed by Testcontainers
        // The @Container annotation will handle the lifecycle
    }
    
    @AfterAll
    static void tearDown() {
        // No need to close container here as it's now managed by AutoCloseable
    }
}
