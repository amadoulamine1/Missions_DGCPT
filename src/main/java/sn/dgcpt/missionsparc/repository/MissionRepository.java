package sn.dgcpt.missionsparc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.dgcpt.missionsparc.domain.Mission;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MissionRepository extends JpaRepository<Mission, Integer> {
    Optional<Mission> findByReference(String reference);
    long countByReferenceStartingWith(String prefixe);

    /** Missions où l'agent est membre et dont la période chevauche [debut, fin] (fin null = ouverte). */
    @Query("select m from Mission m join m.membres a " +
           "where a.matricule = :mat " +
           "and (m.dateFin is null or m.dateFin >= :debut) " +
           "and (:fin is null or m.dateDebut <= :fin)")
    List<Mission> membreEnConflit(@Param("mat") String matricule,
                                  @Param("debut") LocalDate debut,
                                  @Param("fin") LocalDate fin);
}
