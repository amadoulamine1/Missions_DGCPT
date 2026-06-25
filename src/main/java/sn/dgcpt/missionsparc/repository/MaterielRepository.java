package sn.dgcpt.missionsparc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.dgcpt.missionsparc.domain.Materiel;

import java.util.List;
import java.util.Optional;

public interface MaterielRepository extends JpaRepository<Materiel, String> {
    long countByNumeroInventaireStartingWith(String prefixe);
    List<Materiel> findByPoste_Id(Integer posteId);
    long countByPoste_Id(Integer posteId);
    /** Clé naturelle des types génériques (famille AUTRE) : MAC portée par le matériel de base. */
    Optional<Materiel> findByMac(String mac);
}
