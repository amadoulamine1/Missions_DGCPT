package sn.dgcpt.missionsparc.config;

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
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/favicon.ico").permitAll()
                .requestMatchers("/utilisateurs/**").hasRole("ADMIN")
                .requestMatchers("/agents/**", "/postes/**").hasRole("ADMIN")
                .requestMatchers("/missions/nouvelle", "/missions/*/membres/**").hasAnyRole("ADMIN", "CHEF_MISSION")
                .requestMatchers("/import/valider").hasAnyRole("ADMIN", "CHEF_MISSION")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll())
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll())
            // CSRF désactivé pour l'intranet (à réactiver en production)
            .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
