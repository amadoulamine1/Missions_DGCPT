package sn.dgcpt.missionsparc.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "ordinateur")
@Getter
@Setter
@NoArgsConstructor
public class Ordinateur {

    @Id
    private String numeroInventaire;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "numero_inventaire")
    private Materiel materiel;

    @Column(name = "mac_ethernet")
    private String macEthernet;

    @Column(name = "mac_wifi")
    private String macWifi;

    @Column(name = "nom_machine")
    private String nomMachine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_installateur_matricule")
    private Agent agentInstallateur;

    @ManyToMany
    @JoinTable(name = "ordinateur_logiciel",
            joinColumns = @JoinColumn(name = "ordinateur_numero"),
            inverseJoinColumns = @JoinColumn(name = "logiciel_id"))
    private Set<Logiciel> logiciels = new HashSet<>();
}
