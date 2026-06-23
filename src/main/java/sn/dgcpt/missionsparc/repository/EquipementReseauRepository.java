package sn.dgcpt.missionsparc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.dgcpt.missionsparc.domain.EquipementReseau;

import java.util.Optional;

public interface EquipementReseauRepository extends JpaRepository<EquipementReseau, String> {
    Optional<EquipementReseau> findByMac(String mac);
}
