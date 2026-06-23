package sn.dgcpt.missionsparc.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "imprimante")
@Getter
@Setter
@NoArgsConstructor
public class Imprimante {

    @Id
    private String numeroInventaire;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "numero_inventaire")
    private Materiel materiel;

    private String mac;

    @Column(name = "mac_wifi")
    private String macWifi;

    private String ip;
}
