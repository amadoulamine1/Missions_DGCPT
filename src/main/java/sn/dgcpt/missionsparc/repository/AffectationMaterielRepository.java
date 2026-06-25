package sn.dgcpt.missionsparc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.dgcpt.missionsparc.domain.AffectationMateriel;
import sn.dgcpt.missionsparc.domain.Materiel;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AffectationMaterielRepository extends JpaRepository<AffectationMateriel, Integer> {
    Optional<AffectationMateriel> findByMaterielAndDateFinIsNull(Materiel materiel);

    java.util.List<AffectationMateriel> findByMaterielOrderByDateDebutDesc(Materiel materiel);

    List<AffectationMateriel> findByPoste_IdOrderByDateDebutDesc(Integer posteId);

    @Query("select a from AffectationMateriel a where a.dateDebut <= :d and (a.dateFin is null or a.dateFin > :d)")
    List<AffectationMateriel> actives(@Param("d") LocalDate d);
}
