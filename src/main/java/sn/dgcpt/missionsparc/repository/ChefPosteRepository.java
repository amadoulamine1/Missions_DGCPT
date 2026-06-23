package sn.dgcpt.missionsparc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.dgcpt.missionsparc.domain.ChefPoste;

import java.util.Optional;

public interface ChefPosteRepository extends JpaRepository<ChefPoste, Integer> {
    Optional<ChefPoste> findFirstByPoste_IdAndDateFinIsNull(Integer posteId);
}
