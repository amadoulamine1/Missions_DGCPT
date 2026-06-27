package sn.dgcpt.missionsparc.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.dgcpt.missionsparc.domain.AuditEvent;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    /** Recherche paginée du journal (filtre par action et par utilisateur). {@code user} en minuscules ou vide. */
    @Query("""
            select e from AuditEvent e
            where (:action is null or e.action = :action)
              and (:user = '' or lower(coalesce(e.utilisateur, '')) like concat('%', :user, '%'))
            """)
    Page<AuditEvent> rechercher(@Param("action") String action, @Param("user") String user, Pageable pageable);

    @Query("select distinct e.action from AuditEvent e order by e.action")
    List<String> actionsDistinctes();
}
