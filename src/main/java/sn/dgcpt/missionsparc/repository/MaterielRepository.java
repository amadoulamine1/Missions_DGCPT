package sn.dgcpt.missionsparc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.dgcpt.missionsparc.domain.Materiel;

import java.util.List;

public interface MaterielRepository extends JpaRepository<Materiel, String> {
    long countByNumeroInventaireStartingWith(String prefixe);
    List<Materiel> findByPoste_Id(Integer posteId);
    long countByPoste_Id(Integer posteId);
}
