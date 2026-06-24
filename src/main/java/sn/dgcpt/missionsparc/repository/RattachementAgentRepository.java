package sn.dgcpt.missionsparc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.dgcpt.missionsparc.domain.RattachementAgent;

import java.util.List;
import java.util.Optional;

public interface RattachementAgentRepository extends JpaRepository<RattachementAgent, Integer> {
    Optional<RattachementAgent> findFirstByAgent_MatriculeAndDateFinIsNull(String matricule);
    List<RattachementAgent> findByAgent_MatriculeOrderByDateDebutDesc(String matricule);
}
