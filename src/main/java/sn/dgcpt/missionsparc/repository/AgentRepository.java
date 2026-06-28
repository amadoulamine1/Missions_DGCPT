package sn.dgcpt.missionsparc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sn.dgcpt.missionsparc.domain.Agent;
import sn.dgcpt.missionsparc.domain.TypeAgent;

import java.util.List;

public interface AgentRepository extends JpaRepository<Agent, String> {
    List<Agent> findByTypeAgent(TypeAgent typeAgent);
    List<Agent> findByPoste_Id(Integer posteId);

    /** Tous les agents avec leur poste pré-chargé (évite le N+1 sur le nom du poste dans la liste des agents). */
    @Query("select a from Agent a left join fetch a.poste")
    List<Agent> findAllAvecPoste();
}
