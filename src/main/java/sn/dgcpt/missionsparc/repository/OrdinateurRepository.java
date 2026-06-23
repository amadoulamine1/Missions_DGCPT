package sn.dgcpt.missionsparc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.dgcpt.missionsparc.domain.Ordinateur;

import java.util.Optional;

public interface OrdinateurRepository extends JpaRepository<Ordinateur, String> {
    Optional<Ordinateur> findByMacEthernet(String macEthernet);
}
