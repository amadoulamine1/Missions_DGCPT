package sn.dgcpt.missionsparc.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Expose l'utilisateur connecté, son rôle et la page courante à toutes les vues. */
@ControllerAdvice
public class GlobalModelAdvice {

    private Authentication auth() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a instanceof AnonymousAuthenticationToken) return null;
        return a;
    }

    @ModelAttribute("currentUser")
    public String currentUser() {
        Authentication a = auth();
        return a == null ? null : a.getName();
    }

    @ModelAttribute("currentRole")
    public String currentRole() {
        Authentication a = auth();
        if (a == null) return null;
        return a.getAuthorities().stream().findFirst()
                .map(g -> g.getAuthority().replace("ROLE_", "")).orElse(null);
    }

    /** Chemin de la requête courante, pour surligner l'entrée de menu active. */
    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request == null ? "" : request.getRequestURI();
    }
}
