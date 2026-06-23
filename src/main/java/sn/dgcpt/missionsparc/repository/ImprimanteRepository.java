package sn.dgcpt.missionsparc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.dgcpt.missionsparc.domain.Imprimante;

public interface ImprimanteRepository extends JpaRepository<Imprimante, String> {
}
