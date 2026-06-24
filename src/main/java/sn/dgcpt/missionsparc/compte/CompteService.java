package sn.dgcpt.missionsparc.compte;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.dgcpt.missionsparc.domain.Agent;
import sn.dgcpt.missionsparc.domain.RoleUtilisateur;
import sn.dgcpt.missionsparc.domain.Utilisateur;
import sn.dgcpt.missionsparc.repository.AgentRepository;
import sn.dgcpt.missionsparc.repository.UtilisateurRepository;

import java.util.List;

@Service
public class CompteService implements UserDetailsService {

    private final UtilisateurRepository repo;
    private final PasswordEncoder encoder;
    private final AgentRepository agentRepo;

    public CompteService(UtilisateurRepository repo, PasswordEncoder encoder, AgentRepository agentRepo) {
        this.repo = repo;
        this.encoder = encoder;
        this.agentRepo = agentRepo;
    }

    @Transactional(readOnly = true)
    public List<UtilisateurLigne> lister() {
        return repo.findAll().stream()
                .map(u -> new UtilisateurLigne(u.getId(), u.getUsername(),
                        u.getNomComplet() == null ? "" : u.getNomComplet(), u.getRole().name(), u.isActif(),
                        u.getAgent() == null ? null : u.getAgent().getMatricule()))
                .toList();
    }

    @Transactional(readOnly = true)
    public UtilisateurForm trouver(Integer id) {
        Utilisateur u = repo.findById(id).orElseThrow();
        UtilisateurForm f = new UtilisateurForm();
        f.setId(u.getId());
        f.setUsername(u.getUsername());
        f.setNomComplet(u.getNomComplet());
        f.setRole(u.getRole().name());
        f.setActif(u.isActif());
        f.setAgentMatricule(u.getAgent() == null ? null : u.getAgent().getMatricule());
        return f;
    }

    @Transactional
    public void creer(UtilisateurForm f) {
        if (f.getMotDePasse() == null || f.getMotDePasse().isBlank())
            throw new IllegalArgumentException("Le mot de passe est obligatoire.");
        Utilisateur u = new Utilisateur();
        u.setRole(RoleUtilisateur.valueOf(f.getRole()));
        u.setActif(true);
        u.setMotDePasse(encoder.encode(f.getMotDePasse()));
        lierAgent(u, f);
        if (u.getAgent() == null) {   // compte système : identifiant + nom saisis
            String username = f.getUsername() == null ? "" : f.getUsername().trim();
            if (username.isEmpty()) throw new IllegalArgumentException("L'identifiant est obligatoire.");
            u.setUsername(username);
            u.setNomComplet(f.getNomComplet());
        }
        if (repo.existsByUsername(u.getUsername()))
            throw new IllegalArgumentException("L'identifiant « " + u.getUsername() + " » existe déjà.");
        repo.save(u);
    }

    @Transactional
    public void modifier(UtilisateurForm f) {
        Utilisateur u = repo.findById(f.getId()).orElseThrow();
        u.setRole(RoleUtilisateur.valueOf(f.getRole()));
        u.setActif(f.isActif());
        if (f.getMotDePasse() != null && !f.getMotDePasse().isBlank()) {
            u.setMotDePasse(encoder.encode(f.getMotDePasse()));
        }
        lierAgent(u, f);
        if (u.getAgent() == null) {
            u.setNomComplet(f.getNomComplet());
        }
        repo.save(u);
    }

    /** Réinitialise le mot de passe à une valeur temporaire (renvoyée en clair à l'administrateur). */
    @Transactional
    public String reinitialiser(Integer id) {
        Utilisateur u = repo.findById(id).orElseThrow();
        String temp = "Tmp" + (int) (Math.random() * 9000 + 1000);
        u.setMotDePasse(encoder.encode(temp));
        repo.save(u);
        return temp;
    }

    /** Associe (ou détache) un agent informaticien au compte ; le nom est alors dérivé de l'agent. */
    private void lierAgent(Utilisateur u, UtilisateurForm f) {
        String mat = f.getAgentMatricule() == null ? "" : f.getAgentMatricule().trim();
        if (mat.isEmpty()) {
            u.setAgent(null);
            return;
        }
        repo.findByAgent_Matricule(mat).ifPresent(autre -> {
            if (!autre.getId().equals(u.getId()))
                throw new IllegalArgumentException("L'agent « " + mat + " » est déjà lié à un autre compte.");
        });
        Agent a = agentRepo.findById(mat).orElseThrow(() -> new IllegalArgumentException("Agent introuvable : " + mat));
        u.setAgent(a);
        u.setUsername(a.getMatricule());
        u.setNomComplet(a.getNom() + " " + a.getPrenom());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Utilisateur u = repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur inconnu : " + username));
        return User.withUsername(u.getUsername())
                .password(u.getMotDePasse())
                .roles(u.getRole().name())
                .disabled(!u.isActif())
                .build();
    }
}
