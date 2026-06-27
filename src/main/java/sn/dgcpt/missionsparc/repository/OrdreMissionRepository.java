package sn.dgcpt.missionsparc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sn.dgcpt.missionsparc.domain.OrdreMission;

import java.util.List;

public interface OrdreMissionRepository extends JpaRepository<OrdreMission, Integer> {

    /** Métadonnées seules (sans le contenu BYTEA) pour annoter la liste des missions. */
    @Query("select o.id as id, o.missionId as missionId, o.nomFichier as nomFichier "
            + "from OrdreMission o order by o.dateAjout")
    List<OrdreMeta> findAllMeta();

    /** Métadonnées des ordres d'une mission (sans le contenu). */
    @Query("select o.id as id, o.missionId as missionId, o.nomFichier as nomFichier "
            + "from OrdreMission o where o.missionId = :missionId order by o.dateAjout")
    List<OrdreMeta> findMetaByMission(Integer missionId);

    interface OrdreMeta {
        Integer getId();
        Integer getMissionId();
        String getNomFichier();
    }
}
