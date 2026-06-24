package sn.dgcpt.missionsparc.compte;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.dgcpt.missionsparc.domain.Utilisateur;
import sn.dgcpt.missionsparc.repository.UtilisateurRepository;

@Service
public class CompteService implements UserDetailsService {

    private final UtilisateurRepository repo;

    public CompteService(UtilisateurRepository repo) {
        this.repo = repo;
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
