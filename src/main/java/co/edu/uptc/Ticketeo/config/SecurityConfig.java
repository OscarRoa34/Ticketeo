package co.edu.uptc.Ticketeo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {


    @Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/css/**", "/images/**", "/js/**", "/uploads/**").permitAll()
            .requestMatchers("/admin/**", "/categories/**", "/reports/**").hasRole("ADMIN")
            // Quitamos o modificamos la restricción de /user/** para que invitados puedan ver la pagina principal de usuario
            .requestMatchers("/event/interest/**").hasAnyRole("USER", "ADMIN")
            .anyRequest().permitAll()
        )
        .formLogin(login -> login
            .loginPage("/login")
            .defaultSuccessUrl("/", false)
            .permitAll()
        )
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/")
            .permitAll()
        );

    return http.build();
}

@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}}