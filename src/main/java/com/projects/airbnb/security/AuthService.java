package com.projects.airbnb.security;

import com.projects.airbnb.dto.LoginRequestDto;
import com.projects.airbnb.dto.LoginResponseDto;
import com.projects.airbnb.dto.SignUpRequestDto;
import com.projects.airbnb.dto.UserDto;
import com.projects.airbnb.entity.User;
import com.projects.airbnb.entity.enums.Role;
import com.projects.airbnb.exception.ResourceNotFoundException;
import com.projects.airbnb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;

    public UserDto signUp(SignUpRequestDto signUpRequestDto){
        Optional<User> user = userRepository.findByEmail(signUpRequestDto.getEmail());
        if (user.isPresent()){
            throw new RuntimeException("User already registered with email: "+signUpRequestDto.getEmail());
        }

        User newUser = modelMapper.map(signUpRequestDto,User.class);
        newUser.setPassword(passwordEncoder.encode(signUpRequestDto.getPassword()));
        newUser.setRoles(Set.of(Role.GUEST));
        newUser = userRepository.save(newUser);
        log.info("User {}",newUser);
        return modelMapper.map(newUser,UserDto.class);
    }

    public LoginResponseDto login(LoginRequestDto loginRequestDto){
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginRequestDto.getEmail(), loginRequestDto.getPassword())
        );

        User user = (User) authentication.getPrincipal();

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new LoginResponseDto(user.getId(),accessToken,refreshToken);
    }

    public LoginResponseDto refreshToken(String refreshToken) {
        Long userId = jwtService.getUserId(refreshToken);//If the refresh token itself is expired, then while fetching the user id from this method, it will throw jwt expired exception

        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found with id: "+userId));

        //Generate the access token
        String accessToken = jwtService.generateAccessToken(user);

        return new LoginResponseDto(userId,accessToken,refreshToken);
    }

}
