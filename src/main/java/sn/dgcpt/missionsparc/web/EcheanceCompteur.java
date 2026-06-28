package sn.dgcpt.missionsparc.web;

import org.springframework.stereotype.Component;
import sn.dgcpt.missionsparc.domain.StatutMission;
import sn.dgcpt.missionsparc.repository.MissionRepository;

import java.time.LocalDate;

/**
 * Compteur des missions à échéance (non clôturées dont la date de fin est atteinte ou proche),
 * affiché dans le menu sur chaque page. Mis en cache brièvement pour éviter une requête à chaque
 * rendu : la valeur est globale (non personnalisée) et évolue lentement.
 */
@Component
public class EcheanceCompteur {

    /** Fenêtre d'anticipation des échéances (jours). */
    private static final int JOURS_ECHEANCE = 7;
    /** Durée de validité du cache (ms). */
    private static final long TTL_MS = 60_000;

    private final MissionRepository missionRepo;
    private volatile long calculeA = 0L;
    private volatile long valeur = 0L;

    public EcheanceCompteur(MissionRepository missionRepo) {
        this.missionRepo = missionRepo;
    }

    public long compter() {
        long maintenant = System.currentTimeMillis();
        if (maintenant - calculeA > TTL_MS) {
            valeur = missionRepo.countAEcheance(StatutMission.CLOTUREE, LocalDate.now().plusDays(JOURS_ECHEANCE));
            calculeA = maintenant;
        }
        return valeur;
    }
}
