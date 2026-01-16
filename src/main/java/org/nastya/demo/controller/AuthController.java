package org.nastya.demo.controller;

import lombok.RequiredArgsConstructor;
import org.nastya.demo.dto.LoginDto;
import org.nastya.demo.dto.LoginResultDto;
import org.nastya.demo.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResultDto> login(@RequestBody LoginDto authRequest) {
        LoginResultDto response = authService.login(authRequest);
        return ResponseEntity.ok(response);
    }
}