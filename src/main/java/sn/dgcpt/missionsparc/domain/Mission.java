package sn.dgcpt.missionsparc.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "mission")
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

    @Column(name = "observations", length = 2000)
    private String observations;

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

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getObjet() { return objet; }
    public void setObjet(String objet) { this.objet = objet; }
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }
    public Poste getPoste() { return poste; }
    public void setPoste(Poste poste) { this.poste = poste; }
    public Agent getChefMission() { return chefMission; }
    public void setChefMission(Agent chefMission) { this.chefMission = chefMission; }
    public Agent getChefPosteFige() { return chefPosteFige; }
    public void setChefPosteFige(Agent chefPosteFige) { this.chefPosteFige = chefPosteFige; }
    public String getEtatCablage() { return etatCablage; }
    public void setEtatCablage(String etatCablage) { this.etatCablage = etatCablage; }
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
    public CategorieCable getCategorieCable() { return categorieCable; }
    public void setCategorieCable(CategorieCable categorieCable) { this.categorieCable = categorieCable; }
    public StatutMission getStatut() { return statut; }
    public void setStatut(StatutMission statut) { this.statut = statut; }
    public Set<Agent> getMembres() { return membres; }
    public void setMembres(Set<Agent> membres) { this.membres = membres; }
}
