package sn.dgcpt.missionsparc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.dgcpt.missionsparc.domain.Agent;
import sn.dgcpt.missionsparc.domain.TypeAgent;

import java.util.List;

public interface AgentRepository extends JpaRepository<Agent, String> {
    List<Agent> findByTypeAgent(TypeAgent typeAgent);
}
