package com.ead.gearup.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ead.gearup.dto.request.ResendEmailRequestDTO;
import com.ead.gearup.dto.response.JwtTokensDTO;
import com.ead.gearup.dto.response.LoginResponseDTO;
import com.ead.gearup.dto.response.UserResponseDTO;
import com.ead.gearup.dto.user.UserCreateDTO;
import com.ead.gearup.dto.user.UserLoginDTO;
import com.ead.gearup.exception.EmailAlreadyExistsException;
import com.ead.gearup.exception.InvalidRefreshTokenException;
import com.ead.gearup.exception.ResendEmailCooldownException;
import com.ead.gearup.model.User;
import com.ead.gearup.model.UserPrinciple;
import com.ead.gearup.repository.UserRepository;
import com.ead.gearup.service.auth.CustomUserDetailsService;
import com.ead.gearup.service.auth.JwtService;

import io.jsonwebtoken.ExpiredJwtException;

@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest {

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // Any common initialization if needed
    }

    // ======== createUser() Tests ========
    @Test
    void testCreateUserSuccess() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setEmail("test@example.com");
        dto.setName("John Doe");
        dto.setPassword("Password123@");

        when(encoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDTO response = authService.createUser(dto);

        assertEquals("test@example.com", response.getEmail());
        assertEquals("John Doe", response.getName());
        verify(emailVerificationService, times(1)).sendVerificationEmail(any(User.class));
    }

    @Test
    void testCreateUserEmailAlreadyExists() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setEmail("test@example.com");
        dto.setName("John Doe");
        dto.setPassword("password123");

        when(encoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenThrow(DataIntegrityViolationException.class);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.createUser(dto));
    }

    // ======== verifyEmailToken() Tests ========
    @Test
    void testVerifyEmailTokenSuccess() {
        String token = "mockToken";
        User user = new User();
        user.setEmail("test@example.com");
        user.setIsVerified(false);

        when(jwtService.extractUsername(token)).thenReturn("test@example.com");
        when(jwtService.extractClaim(eq(token), any())).thenReturn("email_verification");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        boolean result = authService.verifyEmailToken(token);

        assertTrue(result);
        assertTrue(user.getIsVerified());
    }

    @Test
    void testVerifyEmailTokenExpired() {
        String token = "expiredToken";
        when(jwtService.extractUsername(token)).thenThrow(new ExpiredJwtException(null, null, "expired"));

        boolean result = authService.verifyEmailToken(token);

        assertFalse(result);
    }

    @Test
    void testVerifyEmailTokenUserNotFound() {
        String token = "token";
        when(jwtService.extractUsername(token)).thenReturn("missing@example.com");
        when(jwtService.extractClaim(eq(token), any())).thenReturn("email_verification");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        boolean result = authService.verifyEmailToken(token);
        assertFalse(result);
    }

    // ======== resendEmail() Tests ========
    @Test
    void testResendEmailAlreadyVerified() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setIsVerified(true);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        ResendEmailRequestDTO dto = new ResendEmailRequestDTO();
        dto.setEmail("test@example.com");

        assertThrows(IllegalStateException.class, () -> authService.resendEmail(dto));
    }

    @Test
    void testResendEmailCooldown() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setIsVerified(false);
        user.setLastVerificationEmailSent(LocalDateTime.now());

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        ResendEmailRequestDTO dto = new ResendEmailRequestDTO();
        dto.setEmail("test@example.com");

        assertThrows(ResendEmailCooldownException.class, () -> authService.resendEmail(dto));
    }

    // ======== verifyUser() Tests ========
    @Test
    void testVerifyUserSuccess() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setEmail("test@example.com");
        dto.setPassword("password123");

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);

        UserPrinciple principle = mock(UserPrinciple.class);
        User user = new User();
        when(principle.getUser()).thenReturn(user);
        when(auth.getPrincipal()).thenReturn(principle);

        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtService.generateAccessToken(eq(principle), anyMap())).thenReturn("accessToken");
        when(jwtService.generateRefreshToken(eq(principle))).thenReturn("refreshToken");
        when(userRepository.save(user)).thenReturn(user);

        JwtTokensDTO tokens = authService.verifyUser(dto);

        assertEquals("accessToken", tokens.getAccessToken());
        assertEquals("refreshToken", tokens.getRefreshToken());
    }

    @Test
    void testVerifyUserBadCredentials() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setEmail("test@example.com");
        dto.setPassword("wrongPass");

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

        assertThrows(BadCredentialsException.class, () -> authService.verifyUser(dto));
    }

    // ======== getRefreshAccessToken() Tests ========
    @Test
    void testGetRefreshAccessTokenSuccess() {
        String refreshToken = "refreshToken";
        UserDetails userDetails = mock(UserDetails.class);

        when(jwtService.extractUsername(refreshToken)).thenReturn("test@example.com");
        when(customUserDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
        when(jwtService.validateRefreshToken(refreshToken, userDetails)).thenReturn(true);
        when(jwtService.generateAccessToken(userDetails)).thenReturn("newAccessToken");

        LoginResponseDTO response = authService.getRefreshAccessToken(refreshToken);

        assertEquals("newAccessToken", response.getAccessToken());
    }

    @Test
    void testGetRefreshAccessTokenInvalidToken() {
        String refreshToken = "invalidToken";
        UserDetails userDetails = mock(UserDetails.class);

        when(jwtService.extractUsername(refreshToken)).thenReturn("test@example.com");
        when(customUserDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
        when(jwtService.validateRefreshToken(refreshToken, userDetails)).thenReturn(false);

        assertThrows(InvalidRefreshTokenException.class,
                () -> authService.getRefreshAccessToken(refreshToken));
    }
}
