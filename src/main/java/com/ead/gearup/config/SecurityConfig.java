package com.ead.gearup.config;

import com.ead.gearup.dto.response.ApiResponseDTO;
import com.ead.gearup.filter.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Value("${cors.allowed.origins:http://localhost:3000,http://localhost:3001}")
    private String allowedOrigins;

    /**
     * Main Security Filter Chain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Disable CSRF (stateless JWT)
                .csrf(csrf -> csrf.disable())

                // Enable CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Authorization rules
                .authorizeHttpRequests(request -> request
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/graphql/**",
                                "/graphiql/**",
                                "/vendor/**",
                                "/swagger-ui.html",
                                "/swagger",
                                "/api/v1/auth/**",
                                "/api/v1/public/**",
                                "/api/v1/appointments/shop-settings",
                                "/actuator/**")
                        .permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/chat/**").authenticated() // Chat requires authentication
                        // .requestMatchers("/api/v1/customers/**").hasRole("CUSTOMER")
                        // .requestMatchers("/api/v1/employees/**").hasRole("EMPLOYEE")
                        .anyRequest().authenticated())

                // Stateless session
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Handle unauthenticated (401) and unauthorized (403) responses
                .exceptionHandling(ex -> ex.authenticationEntryPoint((req, res, authEx) -> {
                    res.setContentType("application/json");
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

                    ApiResponseDTO<Object> apiResponse = ApiResponseDTO.builder()
                            .status("error")
                            .message("Unauthorized")
                            .path(req.getRequestURI())
                            .data(null)
                            .build();

                    res.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                })

                        // 403 - Authenticated but insufficient role
                        .accessDeniedHandler((req, res, accessDeniedEx) -> {
                            res.setContentType("application/json");
                            res.setStatus(HttpServletResponse.SC_FORBIDDEN);

                            ApiResponseDTO<Object> apiResponse = ApiResponseDTO.builder()
                                    .status("error")
                                    .message("Forbidden: Access denied")
                                    .path(req.getRequestURI())
                                    .data(null)
                                    .build();

                            res.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                        }))

                // Add JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    /**
     * BCrypt password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * AuthenticationManager (no deprecated .and() / SecurityConfigurerAdapter)
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * CORS configuration
     *
     * SECURITY: Configure allowed origins via environment variable
     *
     * Development: cors.allowed.origins=http://localhost:3000,http://localhost:3001
     * Production: cors.allowed.origins=https://gearup.code102.site,https://your-domain.com
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Parse allowed origins from environment variable
        List<String> origins = Arrays.asList(allowedOrigins.split(","));

        // For development with wildcard ports, convert to patterns
        List<String> patterns = origins.stream()
                .map(origin -> {
                    // Convert localhost URLs to patterns supporting any port
                    if (origin.contains("localhost") && !origin.contains("*")) {
                        return origin.replaceAll(":\\d+", ":*");
                    }
                    return origin;
                })
                .toList();

        config.setAllowedOriginPatterns(patterns);

        config.setAllowedHeaders(List.of("*")); // Allow all headers
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // Cache preflight response for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
