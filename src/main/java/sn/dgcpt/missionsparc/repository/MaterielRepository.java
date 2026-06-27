package sn.dgcpt.missionsparc.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.dgcpt.missionsparc.domain.Materiel;
import sn.dgcpt.missionsparc.domain.StatutMateriel;

import java.util.List;
import java.util.Optional;

public interface MaterielRepository extends JpaRepository<Materiel, String> {
    long countByNumeroInventaireStartingWith(String prefixe);
    List<Materiel> findByPoste_Id(Integer posteId);
    long countByPoste_Id(Integer posteId);
    /** Clé naturelle des types génériques (famille AUTRE) : MAC portée par le matériel de base. */
    Optional<Materiel> findByMac(String mac);

    /**
     * Recherche paginée du parc (filtres + pagination/tri côté base). Le « type » est filtré sur le
     * libellé de catégorie (cohérent avec l'affichage) ; la recherche texte porte sur n°/nom/modèle.
     * {@code q} est attendu déjà en minuscules (ou vide) ; {@code statut}/{@code typeLib} valent {@code null}
     * quand le filtre est inactif.
     */
    @Query("""
            select m from Materiel m
            where (:poste is null or m.poste.id = :poste)
              and (:statut is null or m.statut = :statut)
              and (:typeLib is null or (m.categorie is not null and m.categorie.libelle = :typeLib))
              and (:q = '' or lower(m.numeroInventaire) like concat('%', :q, '%')
                           or lower(coalesce(m.nom, '')) like concat('%', :q, '%')
                           or lower(coalesce(m.modele, '')) like concat('%', :q, '%'))
            """)
    Page<Materiel> rechercher(@Param("poste") Integer poste,
                              @Param("statut") StatutMateriel statut,
                              @Param("typeLib") String typeLib,
                              @Param("q") String q,
                              Pageable pageable);
}
