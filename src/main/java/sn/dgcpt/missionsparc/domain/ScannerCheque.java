package sn.dgcpt.missionsparc.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "scanner_cheque")
@Getter
@Setter
@NoArgsConstructor
public class ScannerCheque {

    @Id
    private String numeroInventaire;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "numero_inventaire")
    private Materiel materiel;

    @Column(name = "numero_serie")
    private String numeroSerie;

    private String marque;
}
