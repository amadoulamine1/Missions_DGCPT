package sn.dgcpt.missionsparc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.dgcpt.missionsparc.domain.LotImport;
import sn.dgcpt.missionsparc.domain.StatutLot;

import java.util.List;

public interface LotImportRepository extends JpaRepository<LotImport, Integer> {
    List<LotImport> findByMission_IdAndStatut(Integer missionId, StatutLot statut);
    List<LotImport> findByMission_Id(Integer missionId);
    List<LotImport> findByMission_Poste_IdOrderByDateChargementDesc(Integer posteId);
}
