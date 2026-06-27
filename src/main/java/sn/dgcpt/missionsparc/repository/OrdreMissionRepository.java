package sn.dgcpt.missionsparc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sn.dgcpt.missionsparc.domain.OrdreMission;

import java.util.List;

public interface OrdreMissionRepository extends JpaRepository<OrdreMission, Integer> {

    /** Métadonnées seules (sans le contenu BYTEA) pour annoter la liste des missions. */
    @Query("select o.missionId as missionId, o.nomFichier as nomFichier from OrdreMission o")
    List<OrdreMeta> findAllMeta();

    interface OrdreMeta {
        Integer getMissionId();
        String getNomFichier();
    }
}
