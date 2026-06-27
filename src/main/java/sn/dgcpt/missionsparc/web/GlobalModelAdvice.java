package sn.dgcpt.missionsparc.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import sn.dgcpt.missionsparc.domain.StatutMission;
import sn.dgcpt.missionsparc.repository.MissionRepository;

import java.time.LocalDate;

/** Expose l'utilisateur connecté, son rôle, la page courante et les notifications à toutes les vues. */
@ControllerAdvice
public class GlobalModelAdvice {

    /** Fenêtre d'anticipation des échéances de mission (jours). */
    private static final int JOURS_ECHEANCE = 7;

    private final MissionRepository missionRepo;

    public GlobalModelAdvice(MissionRepository missionRepo) {
        this.missionRepo = missionRepo;
    }

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

    /**
     * Notification d'échéance : nombre de missions non clôturées dont la date de fin est atteinte ou
     * proche. Calculé uniquement pour les profils qui agissent sur les missions (ADMIN / Chef de mission).
     */
    @ModelAttribute("nbEcheances")
    public long nbEcheances() {
        String role = currentRole();
        if (!"ADMIN".equals(role) && !"CHEF_MISSION".equals(role)) return 0;
        return missionRepo.countAEcheance(StatutMission.CLOTUREE, LocalDate.now().plusDays(JOURS_ECHEANCE));
    }
}
