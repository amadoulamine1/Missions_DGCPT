package sn.dgcpt.missionsparc.agent;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.dgcpt.missionsparc.domain.Agent;
import sn.dgcpt.missionsparc.domain.RattachementAgent;
import sn.dgcpt.missionsparc.domain.TypeAgent;
import sn.dgcpt.missionsparc.repository.RattachementAgentRepository;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Historisation du rattachement agent↔TPR. Centralise l'ouverture/fermeture des périodes pour que
 * tout agent de poste — y compris ceux <b>créés à la volée</b> (import, désignation d'un chef,
 * création de mission) — obtienne sa ligne d'historique de rattachement, et pas seulement après
 * une première édition via le formulaire.
 */
@Service
public class RattachementService {

    private final RattachementAgentRepository rattachementRepo;

    public RattachementService(RattachementAgentRepository rattachementRepo) {
        this.rattachementRepo = rattachementRepo;
    }

    /**
     * Aligne l'historique de rattachement de l'agent sur son poste courant : clôt la période ouverte
     * si le poste a changé et ouvre la nouvelle. No-op pour un informaticien ou si le poste est inchangé.
     */
    @Transactional
    public void synchroniser(Agent a, LocalDate dateEffet) {
        if (a == null || a.getTypeAgent() != TypeAgent.POSTE) return;
        LocalDate d = (dateEffet == null) ? LocalDate.now() : dateEffet;
        Optional<RattachementAgent> ouvert = rattachementRepo.findFirstByAgent_MatriculeAndDateFinIsNull(a.getMatricule());
        Integer posteActuel = (a.getPoste() == null) ? null : a.getPoste().getId();
        Integer posteOuvert = ouvert.map(r -> r.getPoste() == null ? null : r.getPoste().getId()).orElse(null);
        if (Objects.equals(posteActuel, posteOuvert)) return;
        if (ouvert.isPresent()) {
            RattachementAgent r = ouvert.get();
            LocalDate fin = (r.getDateDebut() != null && d.isBefore(r.getDateDebut())) ? r.getDateDebut() : d;
            r.setDateFin(fin);
            rattachementRepo.saveAndFlush(r);
        }
        if (a.getPoste() != null) {
            RattachementAgent r = new RattachementAgent();
            r.setAgent(a);
            r.setPoste(a.getPoste());
            r.setDateDebut(d);
            rattachementRepo.save(r);
        }
    }
}
