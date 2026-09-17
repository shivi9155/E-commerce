package com.kanhacollection.backend.auth;

import com.kanhacollection.backend.auth.dto.*;
import com.kanhacollection.backend.cart.Cart;
import com.kanhacollection.backend.cart.CartRepository;
import com.kanhacollection.backend.exception.BadRequestException;
import com.kanhacollection.backend.exception.DuplicateResourceException;
import com.kanhacollection.backend.exception.UnauthorizedException;
import com.kanhacollection.backend.user.Role;
import com.kanhacollection.backend.user.User;
import com.kanhacollection.backend.user.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sampleUser = User.builder()
                .id(userId)
                .email("user@example.com")
                .fullName("Test User")
                .passwordHash("encodedPassword")
                .phone("9876543210")
                .role(Role.ROLE_USER)
                .enabled(true)
                .emailVerified(true)
                .build();
    }

    @Test
    @DisplayName("Should successfully register user and create cart")
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .email("USER@EXAMPLE.COM")
                .password("password123")
                .fullName("Test User")
                .phone("9876543210")
                .build();

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(tokenProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("access-token-123");
        when(tokenProvider.generateRefreshToken(eq(userId))).thenReturn("refresh-token-123");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AuthResponse result = authService.register(request, response);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access-token-123");
        assertThat(result.getEmail()).isEqualTo("user@example.com");

        verify(cartRepository, times(1)).save(any(Cart.class));
        verify(valueOperations, times(1)).set(eq("auth:refresh_token:" + userId), eq("refresh-token-123"), any(Duration.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException on duplicate email registration")
    void register_DuplicateEmail_ThrowsException() {
        RegisterRequest request = RegisterRequest.builder()
                .email("user@example.com")
                .password("password123")
                .fullName("Test User")
                .build();

        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request, response))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should login user with valid credentials")
    void login_Success() {
        LoginRequest request = LoginRequest.builder()
                .email("USER@EXAMPLE.COM")
                .password("password123")
                .build();

        UserPrincipal principal = UserPrincipal.create(sampleUser);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(tokenProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("access-token-123");
        when(tokenProvider.generateRefreshToken(eq(userId))).thenReturn("refresh-token-123");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AuthResponse result = authService.login(request, response);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access-token-123");
        assertThat(result.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should refresh access token when refresh token in redis is valid")
    void refreshToken_Success() {
        String refreshToken = "valid-refresh-token";
        when(tokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(tokenProvider.getUserIdFromJwt(refreshToken)).thenReturn(userId);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:refresh_token:" + userId)).thenReturn(refreshToken);
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(tokenProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("new-access-token");
        when(tokenProvider.generateRefreshToken(userId)).thenReturn("new-refresh-token");

        AuthResponse result = authService.refreshToken(refreshToken, null, response);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("new-access-token");
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when refresh token is invalid or revoked")
    void refreshToken_Revoked_ThrowsException() {
        String refreshToken = "revoked-refresh-token";
        when(tokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(tokenProvider.getUserIdFromJwt(refreshToken)).thenReturn(userId);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:refresh_token:" + userId)).thenReturn(null);

        assertThatThrownBy(() -> authService.refreshToken(refreshToken, null, response))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    @DisplayName("Should change password successfully when current password matches")
    void changePassword_Success() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldPass")
                .newPassword("newPass123")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("oldPass", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPass123")).thenReturn("newEncodedPassword");

        authService.changePassword(userId, request);

        verify(userRepository, times(1)).save(sampleUser);
        verify(redisTemplate, times(1)).delete("auth:refresh_token:" + userId);
    }

    @Test
    @DisplayName("Should throw BadRequestException when current password does not match")
    void changePassword_InvalidCurrentPassword_ThrowsException() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("wrongPass")
                .newPassword("newPass123")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongPass", "encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(userId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not match");
    }
}
