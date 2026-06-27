package sn.dgcpt.missionsparc.compte;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protection anti-force-brute : compte les échecs de connexion par identifiant et verrouille
 * temporairement le compte après {@link #MAX_ECHECS} tentatives infructueuses, pendant {@link #DUREE}.
 * État volontairement en mémoire (verrou éphémère, non persisté) — réinitialisé au redémarrage.
 */
@Service
public class LoginAttemptService {

    static final int MAX_ECHECS = 5;
    static final Duration DUREE = Duration.ofMinutes(15);

    private static final class Etat {
        int echecs;
        Instant verrouJusqua;
    }

    private final ConcurrentHashMap<String, Etat> parIdentifiant = new ConcurrentHashMap<>();

    private static String cle(String identifiant) {
        return identifiant == null ? "" : identifiant.trim().toLowerCase();
    }

    /** Enregistre un échec ; déclenche le verrou au-delà du seuil. */
    public void echec(String identifiant) {
        parIdentifiant.compute(cle(identifiant), (k, e) -> {
            if (e == null) e = new Etat();
            e.echecs++;
            if (e.echecs >= MAX_ECHECS) e.verrouJusqua = Instant.now().plus(DUREE);
            return e;
        });
    }

    /** Réinitialise le compteur après une connexion réussie. */
    public void succes(String identifiant) {
        parIdentifiant.remove(cle(identifiant));
    }

    /** Vrai si le compte est actuellement verrouillé ; purge le verrou expiré. */
    public boolean estVerrouille(String identifiant) {
        Etat e = parIdentifiant.get(cle(identifiant));
        if (e == null || e.verrouJusqua == null) return false;
        if (Instant.now().isBefore(e.verrouJusqua)) return true;
        parIdentifiant.remove(cle(identifiant)); // verrou expiré
        return false;
    }
}
