package sn.dgcpt.missionsparc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.dgcpt.missionsparc.domain.Logiciel;

import java.util.Optional;

public interface LogicielRepository extends JpaRepository<Logiciel, Integer> {
    Optional<Logiciel> findByNom(String nom);
}
