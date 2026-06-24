package sn.dgcpt.missionsparc.domain;

import jakarta.persistence.*;

import java.time.Instant;

/** Un fichier canevas chargé pour une mission, en attente de consolidation/intégration. */
@Entity
@Table(name = "lot_import")
public class LotImport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @Column(name = "agent_saisisseur")
    private String agentSaisisseur;

    @Column(name = "source_fichier")
    private String sourceFichier;

    @Column(name = "date_chargement")
    private Instant dateChargement;

    @Column(name = "fichier")
    private byte[] fichier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutLot statut = StatutLot.EN_ATTENTE;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Mission getMission() { return mission; }
    public void setMission(Mission mission) { this.mission = mission; }
    public String getAgentSaisisseur() { return agentSaisisseur; }
    public void setAgentSaisisseur(String agentSaisisseur) { this.agentSaisisseur = agentSaisisseur; }
    public String getSourceFichier() { return sourceFichier; }
    public void setSourceFichier(String sourceFichier) { this.sourceFichier = sourceFichier; }
    public Instant getDateChargement() { return dateChargement; }
    public void setDateChargement(Instant dateChargement) { this.dateChargement = dateChargement; }
    public byte[] getFichier() { return fichier; }
    public void setFichier(byte[] fichier) { this.fichier = fichier; }
    public StatutLot getStatut() { return statut; }
    public void setStatut(StatutLot statut) { this.statut = statut; }
}
