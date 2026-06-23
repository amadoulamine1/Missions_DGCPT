package sn.dgcpt.missionsparc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.dgcpt.missionsparc.domain.Imprimante;

import java.util.Optional;

public interface ImprimanteRepository extends JpaRepository<Imprimante, String> {
    Optional<Imprimante> findByMac(String mac);
}
