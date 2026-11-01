package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.dto.LoginRequestDTO;
import com.isaacggr.finance_app.dto.LoginResponseDTO;
import com.isaacggr.finance_app.exception.UnauthorizedException;
import com.isaacggr.finance_app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final long expirationMillis;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            @Value("${jwt.expiration:86400000}") long expirationMillis
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.expirationMillis = expirationMillis;
    }

    public LoginResponseDTO login(LoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.email())
                .filter(User::isActive)
                .orElseThrow(() -> new UnauthorizedException("Credenciais inválidas"));

        if (!passwordEncoder.matches(dto.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Credenciais inválidas");
        }

        String token = tokenService.generateToken(user);
        return LoginResponseDTO.of(token, expirationMillis);
    }
}

