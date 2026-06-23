package sn.dgcpt.missionsparc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.dgcpt.missionsparc.domain.Logiciel;

public interface LogicielRepository extends JpaRepository<Logiciel, Integer> {
}
