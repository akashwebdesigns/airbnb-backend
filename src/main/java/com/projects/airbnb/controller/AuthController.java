package com.projects.airbnb.controller;

import com.projects.airbnb.dto.LoginRequestDto;
import com.projects.airbnb.dto.LoginResponseDto;
import com.projects.airbnb.dto.SignUpRequestDto;
import com.projects.airbnb.dto.UserDto;
import com.projects.airbnb.security.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "Create a new account", tags = {"Auth"})
    public ResponseEntity<UserDto> signup(@RequestBody SignUpRequestDto signUpRequestDto) {
        return new ResponseEntity<>(authService.signUp(signUpRequestDto), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Login request", tags = {"Auth"})
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto loginRequestDto, HttpServletResponse httpServletResponse){
        LoginResponseDto loginResponseDto = authService.login(loginRequestDto);

        Cookie cookie = new Cookie("refreshToken",loginResponseDto.getRefreshToken());
        cookie.setHttpOnly(true);

        httpServletResponse.addCookie(cookie);

        return ResponseEntity.ok(loginResponseDto);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh the JWT Access Token with a refresh token", tags = {"Auth"})
    public ResponseEntity<LoginResponseDto> refresh(HttpServletRequest request) {
        String refreshToken = Arrays.stream(request.getCookies()).
                filter(cookie -> "refreshToken".equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElseThrow(() -> new AuthenticationServiceException("Refresh token not found inside the Cookies"));

        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }
}
