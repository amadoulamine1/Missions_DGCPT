package sn.dgcpt.missionsparc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.dgcpt.missionsparc.domain.AffectationMateriel;
import sn.dgcpt.missionsparc.domain.Materiel;

import java.util.Optional;

public interface AffectationMaterielRepository extends JpaRepository<AffectationMateriel, Integer> {
    Optional<AffectationMateriel> findByMaterielAndDateFinIsNull(Materiel materiel);
}
