package sn.dgcpt.missionsparc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.dgcpt.missionsparc.domain.Materiel;
import sn.dgcpt.missionsparc.domain.Mission;
import sn.dgcpt.missionsparc.domain.ReleveMateriel;

import java.util.List;
import java.util.Optional;

public interface ReleveMaterielRepository extends JpaRepository<ReleveMateriel, Integer> {
    Optional<ReleveMateriel> findByMissionAndMateriel(Mission mission, Materiel materiel);
    List<ReleveMateriel> findByMission_Id(Integer missionId);
}
