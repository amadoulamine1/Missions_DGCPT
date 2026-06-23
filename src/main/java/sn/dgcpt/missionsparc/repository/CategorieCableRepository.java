package sn.dgcpt.missionsparc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.dgcpt.missionsparc.domain.CategorieCable;

import java.util.Optional;

public interface CategorieCableRepository extends JpaRepository<CategorieCable, Integer> {
    Optional<CategorieCable> findByLibelle(String libelle);
}
