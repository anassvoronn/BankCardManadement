package org.nastya.demo.service;

import lombok.RequiredArgsConstructor;
import org.nastya.demo.dto.LoginDto;
import org.nastya.demo.dto.LoginResultDto;
import org.nastya.demo.entity.User;
import org.nastya.demo.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;

    public LoginResultDto login(LoginDto authRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.username(),
                        authRequest.password()
                )
        );

        User user = userRepository.findByUsername(authRequest.username())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        UserDetails userDetails =
                customUserDetailsService.loadUserByUsername(user.getUsername());

        String token = jwtService.generateToken(userDetails);
        return new LoginResultDto(token);
    }
}