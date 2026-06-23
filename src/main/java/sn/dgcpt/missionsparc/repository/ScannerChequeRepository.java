package sn.dgcpt.missionsparc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.dgcpt.missionsparc.domain.ScannerCheque;

public interface ScannerChequeRepository extends JpaRepository<ScannerCheque, String> {
}
