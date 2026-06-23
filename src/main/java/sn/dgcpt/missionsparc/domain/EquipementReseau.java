package sn.dgcpt.missionsparc.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "equipement_reseau")
@Getter
@Setter
@NoArgsConstructor
public class EquipementReseau {

    @Id
    private String numeroInventaire;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "numero_inventaire")
    private Materiel materiel;

    private String mac;
    private String ip;
}
