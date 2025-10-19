package com.ead.gearup.config;

import com.ead.gearup.dto.response.ApiResponseDTO;
import com.ead.gearup.filter.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

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

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

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
                                "/swagger-ui.html",
                                "/api/v1/auth/**",
                                "/api/v1/public/**",
                                "/actuator/**")
                        .permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
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
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:3000", // dev frontend
                "https://your-production.com" // production frontend
        ));
        config.setAllowedHeaders(List.of("*")); // Allow all headers
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
