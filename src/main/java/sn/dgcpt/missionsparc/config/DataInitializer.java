package sn.dgcpt.missionsparc.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import sn.dgcpt.missionsparc.domain.RoleUtilisateur;
import sn.dgcpt.missionsparc.domain.Utilisateur;
import sn.dgcpt.missionsparc.repository.UtilisateurRepository;

/** Crée un compte administrateur initial si aucun compte n'existe. */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UtilisateurRepository repo;
    private final PasswordEncoder encoder;

    public DataInitializer(UtilisateurRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (repo.count() == 0) {
            Utilisateur admin = new Utilisateur();
            admin.setUsername("admin");
            admin.setMotDePasse(encoder.encode("admin"));
            admin.setRole(RoleUtilisateur.ADMIN);
            admin.setActif(true);
            admin.setNomComplet("Administrateur");
            repo.save(admin);
            log.warn("Compte administrateur initial créé : admin / admin — À CHANGER après la première connexion.");
        }
    }
}
