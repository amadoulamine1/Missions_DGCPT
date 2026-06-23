package sn.dgcpt.missionsparc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.dgcpt.missionsparc.domain.Mission;

import java.util.Optional;

public interface MissionRepository extends JpaRepository<Mission, Integer> {
    Optional<Mission> findByReference(String reference);
    long countByReferenceStartingWith(String prefixe);
}
