package sn.dgcpt.missionsparc.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import sn.dgcpt.missionsparc.compte.CompteService;

/**
 * Forçage du changement de mot de passe : tant qu'un utilisateur connecté porte le drapeau
 * « mot de passe à changer », toute navigation est redirigée vers la page de changement
 * (la page elle-même, la déconnexion et les ressources statiques sont exclues).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CompteService compteService;

    public WebConfig(CompteService compteService) {
        this.compteService = compteService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
                    return true;
                }
                if (compteService.doitChangerMotDePasse(auth.getName())) {
                    resp.sendRedirect(req.getContextPath() + "/mon-mot-de-passe");
                    return false;
                }
                return true;
            }
        }).excludePathPatterns("/mon-mot-de-passe", "/login", "/logout", "/css/**", "/js/**", "/favicon.ico", "/error");
    }
}
