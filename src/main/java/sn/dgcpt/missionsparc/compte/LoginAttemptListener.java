package sn.dgcpt.missionsparc.compte;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/** Relie les événements d'authentification Spring Security au {@link LoginAttemptService}. */
@Component
public class LoginAttemptListener {

    private final LoginAttemptService tentatives;

    public LoginAttemptListener(LoginAttemptService tentatives) {
        this.tentatives = tentatives;
    }

    @EventListener
    public void surEchec(AuthenticationFailureBadCredentialsEvent e) {
        tentatives.echec(e.getAuthentication().getName());
    }

    @EventListener
    public void surSucces(AuthenticationSuccessEvent e) {
        tentatives.succes(e.getAuthentication().getName());
    }
}
