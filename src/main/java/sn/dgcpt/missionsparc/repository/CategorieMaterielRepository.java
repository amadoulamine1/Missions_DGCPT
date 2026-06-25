package sn.dgcpt.missionsparc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.dgcpt.missionsparc.domain.CategorieMateriel;
import sn.dgcpt.missionsparc.domain.TypeMateriel;

import java.util.List;
import java.util.Optional;

public interface CategorieMaterielRepository extends JpaRepository<CategorieMateriel, Integer> {
    Optional<CategorieMateriel> findByLibelle(String libelle);
    Optional<CategorieMateriel> findByLibelleIgnoreCase(String libelle);
    Optional<CategorieMateriel> findByPrefixe(String prefixe);
    Optional<CategorieMateriel> findFirstByFamilleAndSystemeTrue(TypeMateriel famille);
    List<CategorieMateriel> findByActifTrueOrderByLibelle();
    List<CategorieMateriel> findByFamilleAndActifTrueOrderByLibelle(TypeMateriel famille);
}
