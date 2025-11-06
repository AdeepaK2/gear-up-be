package com.ead.gearup.integration.graphql;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ead.gearup.controller.GraphQLHealthCheckController;

@SpringBootTest
class GraphQLHealthCheckControllerTest {

    @Autowired
    private GraphQLHealthCheckController graphQLHealthCheckController;

    @Test
    void testHealthCheck_Success() {
        // Act
        String result = graphQLHealthCheckController.healthCheck();

        // Assert
        assert result != null;
        assert result.equals("GraphQL is working successfully");
    }

    @Test
    void testHealthCheck_ReturnsNonNullValue() {
        // Act
        String result = graphQLHealthCheckController.healthCheck();

        // Assert
        assert result != null;
        assert !result.isEmpty();
    }

    @Test
    void testHealthCheck_ReturnsCorrectMessage() {
        // Act
        String result = graphQLHealthCheckController.healthCheck();

        // Assert
        assert result.contains("GraphQL");
        assert result.contains("working");
        assert result.contains("successfully");
    }

    @Test
    void testHealthCheck_MultipleCalls_ReturnsSameResult() {
        // Act
        String result1 = graphQLHealthCheckController.healthCheck();
        String result2 = graphQLHealthCheckController.healthCheck();
        String result3 = graphQLHealthCheckController.healthCheck();

        // Assert - Test that health check is idempotent
        assert result1.equals(result2);
        assert result2.equals(result3);
        assert result1.equals("GraphQL is working successfully");
    }

    @Test
    void testHealthCheck_MessageFormat() {
        // Act
        String result = graphQLHealthCheckController.healthCheck();

        // Assert
        assert result instanceof String;
        assert result.length() > 0;
        assert result.matches(".*GraphQL.*");
    }
}

