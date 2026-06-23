package sn.dgcpt.missionsparc.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "releve_materiel")
@Getter
@Setter
@NoArgsConstructor
public class ReleveMateriel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materiel_numero", nullable = false)
    private Materiel materiel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_saisisseur_matricule")
    private Agent agentSaisisseur;

    private String zone;

    @Column(name = "source_fichier")
    private String sourceFichier;

    @Column(name = "date_releve", nullable = false)
    private LocalDate dateReleve;

    // TODO: etat_observe (JSONB) — snapshot des attributs observes (photo datee)
}
