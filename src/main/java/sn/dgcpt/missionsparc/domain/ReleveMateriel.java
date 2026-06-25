package sn.dgcpt.missionsparc.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "releve_materiel")
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

    /** Agent informaticien ayant traité le matériel lors de cette mission (historisé par relevé). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_traitant_matricule")
    private Agent agentTraitant;

    private String zone;

    @Column(name = "source_fichier")
    private String sourceFichier;

    @Column(name = "date_releve", nullable = false)
    private LocalDate dateReleve;

    @Column(name = "etat_observe", columnDefinition = "text")
    private String etatObserve;

    /** Statut du matériel observé lors de ce relevé (historique des statuts par mission). */
    @Enumerated(EnumType.STRING)
    @Column(name = "statut_observe")
    private StatutMateriel statutObserve;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Mission getMission() { return mission; }
    public void setMission(Mission mission) { this.mission = mission; }
    public Materiel getMateriel() { return materiel; }
    public void setMateriel(Materiel materiel) { this.materiel = materiel; }
    public Agent getAgentSaisisseur() { return agentSaisisseur; }
    public void setAgentSaisisseur(Agent agentSaisisseur) { this.agentSaisisseur = agentSaisisseur; }
    public Agent getAgentTraitant() { return agentTraitant; }
    public void setAgentTraitant(Agent agentTraitant) { this.agentTraitant = agentTraitant; }
    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }
    public String getSourceFichier() { return sourceFichier; }
    public void setSourceFichier(String sourceFichier) { this.sourceFichier = sourceFichier; }
    public LocalDate getDateReleve() { return dateReleve; }
    public void setDateReleve(LocalDate dateReleve) { this.dateReleve = dateReleve; }
    public String getEtatObserve() { return etatObserve; }
    public void setEtatObserve(String etatObserve) { this.etatObserve = etatObserve; }
    public StatutMateriel getStatutObserve() { return statutObserve; }
    public void setStatutObserve(StatutMateriel statutObserve) { this.statutObserve = statutObserve; }
}
