package sn.dgcpt.missionsparc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

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
                // Supervision : sonde de santé ouverte (réseau/sonde externe), le reste réservé à l'ADMIN
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
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
            // En-têtes de sécurité HTTP. CSP adaptée au rendu serveur (Thymeleaf) avec scripts/styles
            // inline de première main ('unsafe-inline') ; aucune origine externe autorisée. HSTS actif
            // sur les réponses HTTPS (profil prod). Referrer limité à la même origine.
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; "
                        + "script-src 'self' 'unsafe-inline'; font-src 'self'; object-src 'none'; "
                        + "base-uri 'self'; form-action 'self'; frame-ancestors 'self'"))
                .referrerPolicy(ref -> ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000)))
            // CSRF actif : protection des formulaires POST. Les formulaires Thymeleaf (th:action)
            // injectent automatiquement le jeton _csrf (login, logout et tous les formulaires inclus).
            .csrf(org.springframework.security.config.Customizer.withDefaults());
        return http.build();
    }
}
