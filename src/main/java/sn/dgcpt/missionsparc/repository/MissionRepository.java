package sn.dgcpt.missionsparc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.dgcpt.missionsparc.domain.Mission;
import sn.dgcpt.missionsparc.domain.StatutMission;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MissionRepository extends JpaRepository<Mission, Integer> {
    Optional<Mission> findByReference(String reference);
    long countByReferenceStartingWith(String prefixe);
    List<Mission> findByPoste_IdOrderByDateDebutDesc(Integer posteId);

    /** Missions non clôturées dont la date de fin est atteinte ou proche (≤ {@code limite}) : à traiter. */
    @Query("select count(m) from Mission m where m.statut <> :clos and m.dateFin is not null and m.dateFin <= :limite")
    long countAEcheance(@Param("clos") StatutMission clos, @Param("limite") LocalDate limite);

    /**
     * Missions où l'agent est membre et dont la période chevauche [debut, fin].
     * La fin ouverte (sans date de fin) est passée sous forme de date sentinelle par l'appelant,
     * afin d'éviter un paramètre non typable (PostgreSQL 42P18) dans un test IS NULL.
     */
    @Query("select m from Mission m join m.membres a " +
           "where a.matricule = :mat " +
           "and (m.dateFin is null or m.dateFin >= :debut) " +
           "and m.dateDebut <= :fin")
    List<Mission> membreEnConflit(@Param("mat") String matricule,
                                  @Param("debut") LocalDate debut,
                                  @Param("fin") LocalDate fin);
}
