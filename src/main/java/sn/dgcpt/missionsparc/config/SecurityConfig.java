package sn.dgcpt.missionsparc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
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
                // Rapport annuel : document de pilotage (lecture) — ADMIN + MANAGER
                .requestMatchers("/rapport-annuel/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/utilisateurs/**").hasRole("ADMIN")
                .requestMatchers("/referentiels/**").hasRole("ADMIN")
                // Aide à l'import : ouverte ; l'import lui-même reste hors MANAGER (lecture seule)
                .requestMatchers("/import/guide").authenticated()
                .requestMatchers("/import", "/import/**").hasAnyRole("ADMIN", "CHEF_MISSION", "AGENT")
                // Liste des agents : lecture pour ADMIN + MANAGER ; création/édition réservées à ADMIN
                .requestMatchers(HttpMethod.GET, "/agents").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/agents/**").hasRole("ADMIN")
                .requestMatchers("/postes/nouveau", "/postes/*/modifier").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/postes", "/postes/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/postes", "/postes/*").authenticated()
                .requestMatchers("/missions/nouvelle", "/missions/*/modifier", "/missions/*/membres/**").hasAnyRole("ADMIN", "CHEF_MISSION")
                // Ordre de mission : joindre / supprimer réservés à ADMIN + Chef (le téléchargement GET reste authentifié)
                .requestMatchers(HttpMethod.POST, "/missions/*/ordre", "/missions/*/ordre/**").hasAnyRole("ADMIN", "CHEF_MISSION")
                .requestMatchers(HttpMethod.POST, "/missions/*/consolidation/**").hasAnyRole("ADMIN", "CHEF_MISSION")
                .requestMatchers(HttpMethod.POST, "/missions/*/cloturer").hasAnyRole("ADMIN", "CHEF_MISSION")
                .requestMatchers(HttpMethod.POST, "/parc/*/affectation").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll())
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll())
            // CSRF actif : protection des formulaires POST. Les formulaires Thymeleaf (th:action)
            // injectent automatiquement le jeton _csrf (login, logout et tous les formulaires inclus).
            .csrf(org.springframework.security.config.Customizer.withDefaults());
        return http.build();
    }
}
