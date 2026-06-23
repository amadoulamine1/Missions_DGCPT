package sn.dgcpt.missionsparc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.dgcpt.missionsparc.domain.Poste;

import java.util.Optional;

public interface PosteRepository extends JpaRepository<Poste, Integer> {
    Optional<Poste> findByCode(String code);
}
