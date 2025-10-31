package com.ead.gearup.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/health-check")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Health Check", description = "Application health and status monitoring")
public class HealthCheckController {

    @GetMapping
    @Operation(
        summary = "Health check endpoint",
        description = "Returns the current status of the application to verify it's running properly"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Application is healthy",
            content = @Content(
                mediaType = "text/plain",
                examples = @ExampleObject(value = "gearup-backend is running successfully")
            )
        )
    })
    // @RequiresRole({ UserRole.CUSTOMER })
    public ResponseEntity<String> healthCheck() {
        String message = "gearup-backend is running successfully";
        return ResponseEntity.status(HttpStatus.OK).body(message);
    }
}
