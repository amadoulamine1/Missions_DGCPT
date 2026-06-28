package sn.dgcpt.missionsparc.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.dgcpt.missionsparc.domain.Mission;
import sn.dgcpt.missionsparc.domain.StatutMission;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MissionRepository extends JpaRepository<Mission, Integer> {

    /**
     * Recherche paginée des missions (filtres + pagination/tri côté base). L'<b>état temporel</b>
     * (Planifiée / En cours / Terminée) est exprimé en prédicats de dates par rapport à {@code today} ;
     * le filtre « agent » porte sur les membres. {@code q} attendu en minuscules (ou vide) ;
     * {@code poste}/{@code region}/{@code agent}/{@code etat} valent {@code null} quand inactifs.
     */
    @Query("""
            select m from Mission m
            where (:poste is null or m.poste.id = :poste)
              and (:region is null or m.poste.region = :region)
              and (:agent is null or exists (select a from m.membres a where a.matricule = :agent))
              and (:etat is null
                   or (:etat = 'Planifiée' and m.dateDebut > :today)
                   or (:etat = 'En cours'  and m.dateDebut <= :today and (m.dateFin is null or m.dateFin >= :today))
                   or (:etat = 'Terminée'  and m.dateDebut <= :today and m.dateFin is not null and m.dateFin < :today))
              and (:q = '' or lower(m.reference) like concat('%', :q, '%') or lower(m.objet) like concat('%', :q, '%'))
            """)
    Page<Mission> rechercher(@Param("poste") Integer poste, @Param("region") String region,
                             @Param("agent") String agent, @Param("etat") String etat,
                             @Param("today") LocalDate today, @Param("q") String q, Pageable pageable);

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
