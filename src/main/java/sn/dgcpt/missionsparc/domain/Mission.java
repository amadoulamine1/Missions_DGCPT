package sn.dgcpt.missionsparc.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "mission")
@Getter
@Setter
@NoArgsConstructor
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    private String objet;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poste_id", nullable = false)
    private Poste poste;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chef_mission_matricule", nullable = false)
    private Agent chefMission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chef_poste_fige_matricule", nullable = false)
    private Agent chefPosteFige;

    @Column(name = "etat_cablage")
    private String etatCablage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_cable_id")
    private CategorieCable categorieCable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutMission statut = StatutMission.EN_CONSOLIDATION;

    @ManyToMany
    @JoinTable(name = "mission_membre",
            joinColumns = @JoinColumn(name = "mission_id"),
            inverseJoinColumns = @JoinColumn(name = "agent_matricule"))
    private Set<Agent> membres = new HashSet<>();
}
