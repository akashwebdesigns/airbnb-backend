package com.projects.airbnb.security;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final HandlerExceptionResolver handlerExceptionResolver;
    private final JWTAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception{
         httpSecurity.csrf(AbstractHttpConfigurer::disable)
                 .sessionManagement((sessionConfig)->sessionConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                 .authorizeHttpRequests((auth)->auth
                         .requestMatchers("/admin/**").hasRole("HOTEL_MANAGER")
                         .requestMatchers("/booking/**").authenticated()
                         .requestMatchers("/users/**").authenticated()
                         .anyRequest().permitAll()
                 )
                 .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                 .exceptionHandling((exHandlingConfig)->{
                     exHandlingConfig.accessDeniedHandler(accessDeniedHandler())//for 403 exceptions
                             .authenticationEntryPoint(authenticationEntryPoint());//for 401 exceptions

                 });

        return httpSecurity.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception{
        return authenticationConfiguration.getAuthenticationManager();
    }

    //This access denied exception handler will catch the AccessDeniedException and will bridge it to the GlobalExceptionHandler
    //If we will not create this AccessDeniedHandler, then SpringSecurity's default AccessDeniedHandler will be used,
    // which generally send an HTML page instead of a readable JSON response
    @Bean
    public AccessDeniedHandler accessDeniedHandler(){
        return (request,response,accessDeniedException)->{
                handlerExceptionResolver.resolveException(request,response,null,accessDeniedException);
        };
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, ex) -> {
            handlerExceptionResolver.resolveException(request, response, null, ex);
        };
    }
}

