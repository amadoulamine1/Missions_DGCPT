package sn.dgcpt.missionsparc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.dgcpt.missionsparc.domain.Ordinateur;

public interface OrdinateurRepository extends JpaRepository<Ordinateur, String> {
}
