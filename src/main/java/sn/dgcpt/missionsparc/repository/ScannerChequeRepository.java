package sn.dgcpt.missionsparc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.dgcpt.missionsparc.domain.ScannerCheque;

import java.util.Optional;

public interface ScannerChequeRepository extends JpaRepository<ScannerCheque, String> {
    Optional<ScannerCheque> findByNumeroSerie(String numeroSerie);
}
