package org.nastya.demo.service;

import lombok.RequiredArgsConstructor;
import org.nastya.demo.dto.LoginDto;
import org.nastya.demo.dto.LoginResultDto;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AuthService {
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

        UserDetails userDetails =
                customUserDetailsService.loadUserByUsername(authRequest.username());

        String token = jwtService.generateToken(userDetails);
        return new LoginResultDto(token);
    }
}