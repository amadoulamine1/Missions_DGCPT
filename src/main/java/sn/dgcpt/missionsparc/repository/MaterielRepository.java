package sn.dgcpt.missionsparc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.dgcpt.missionsparc.domain.Materiel;

public interface MaterielRepository extends JpaRepository<Materiel, String> {
}
