package sn.dgcpt.missionsparc.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.dgcpt.missionsparc.consultation.dto.PageVue;
import sn.dgcpt.missionsparc.domain.AuditEvent;
import sn.dgcpt.missionsparc.repository.AuditEventRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Journal d'audit : enregistre les actions sensibles et offre leur consultation.
 * L'écriture est best-effort (une défaillance d'audit ne doit pas faire échouer l'action métier).
 */
@Service
public class AuditService {

    // Libellés d'action (stockés tels quels et affichés)
    public static final String CONNEXION = "Connexion";
    public static final String CONNEXION_ECHEC = "Connexion refusée";
    public static final String COMPTE_VERROUILLE = "Compte verrouillé";
    public static final String MISSION_CREEE = "Mission créée";
    public static final String MISSION_CLOTUREE = "Mission clôturée";
    public static final String MATERIEL_REAFFECTE = "Matériel réaffecté";
    public static final String COMPTE_CREE = "Compte créé";
    public static final String COMPTE_MODIFIE = "Compte modifié";
    public static final String MDP_REINITIALISE = "Mot de passe réinitialisé";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final AuditEventRepository repo;

    public AuditService(AuditEventRepository repo) {
        this.repo = repo;
    }

    /** Trace une action attribuée à l'utilisateur courant (déduit du contexte de sécurité). */
    public void tracer(String action, String cible, String detail) {
        tracer(utilisateurCourant(), action, cible, detail);
    }

    /** Trace une action en précisant l'utilisateur (ex. événements de connexion, hors contexte établi). */
    @Transactional
    public void tracer(String utilisateur, String action, String cible, String detail) {
        try {
            AuditEvent e = new AuditEvent();
            e.setDateHeure(Instant.now());
            e.setUtilisateur(tronquer(utilisateur, 120));
            e.setAction(action);
            e.setCible(tronquer(cible, 255));
            e.setDetail(tronquer(detail, 1000));
            repo.save(e);
        } catch (RuntimeException ignore) {
            // best-effort : ne jamais faire échouer l'action métier à cause de l'audit
        }
    }

    @Transactional(readOnly = true)
    public List<String> actions() {
        return repo.actionsDistinctes();
    }

    @Transactional(readOnly = true)
    public PageVue<AuditVue> journal(String action, String user, Pageable pageable) {
        String act = (action == null || action.isBlank()) ? null : action;
        String u = (user == null) ? "" : user.trim().toLowerCase();
        Page<AuditEvent> page = repo.rechercher(act, u, pageable);
        List<AuditVue> contenu = page.getContent().stream().map(this::vers).toList();
        return new PageVue<>(contenu, page.getNumber(), page.getSize(),
                (int) page.getTotalElements(), Math.max(1, page.getTotalPages()));
    }

    private AuditVue vers(AuditEvent e) {
        String dt = e.getDateHeure() == null ? ""
                : LocalDateTime.ofInstant(e.getDateHeure(), ZoneId.systemDefault()).format(FMT);
        return new AuditVue(dt, nz(e.getUtilisateur()), e.getAction(), nz(e.getCible()), nz(e.getDetail()));
    }

    private String utilisateurCourant() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a instanceof AnonymousAuthenticationToken || !a.isAuthenticated()) return null;
        return a.getName();
    }

    private String nz(String s) { return s == null ? "" : s; }
    private String tronquer(String s, int max) { return (s != null && s.length() > max) ? s.substring(0, max) : s; }
}
