package sn.dgcpt.missionsparc.compte;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import sn.dgcpt.missionsparc.audit.AuditService;

/** Relie les événements d'authentification au {@link LoginAttemptService} et au journal d'audit. */
@Component
public class LoginAttemptListener {

    private final LoginAttemptService tentatives;
    private final AuditService audit;

    public LoginAttemptListener(LoginAttemptService tentatives, AuditService audit) {
        this.tentatives = tentatives;
        this.audit = audit;
    }

    @EventListener
    public void surEchec(AuthenticationFailureBadCredentialsEvent e) {
        String identifiant = e.getAuthentication().getName();
        tentatives.echec(identifiant);
        String ip = ip(e.getAuthentication().getDetails());
        if (tentatives.estVerrouille(identifiant)) {
            audit.tracer(identifiant, AuditService.COMPTE_VERROUILLE, identifiant,
                    "Verrouillage temporaire après échecs répétés" + (ip == null ? "" : " · IP " + ip));
        } else {
            audit.tracer(identifiant, AuditService.CONNEXION_ECHEC, identifiant, ip == null ? null : "IP " + ip);
        }
    }

    @EventListener
    public void surSucces(AuthenticationSuccessEvent e) {
        String identifiant = e.getAuthentication().getName();
        tentatives.succes(identifiant);
        String ip = ip(e.getAuthentication().getDetails());
        audit.tracer(identifiant, AuditService.CONNEXION, null, ip == null ? null : "IP " + ip);
    }

    private String ip(Object details) {
        return (details instanceof WebAuthenticationDetails w) ? w.getRemoteAddress() : null;
    }
}
