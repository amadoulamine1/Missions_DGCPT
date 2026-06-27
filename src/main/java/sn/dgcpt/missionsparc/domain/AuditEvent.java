package sn.dgcpt.missionsparc.domain;

import jakarta.persistence.*;

import java.time.Instant;

/** Entrée du journal d'audit : une action sensible horodatée et attribuée à un utilisateur. */
@Entity
@Table(name = "audit_event")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_heure", nullable = false)
    private Instant dateHeure;

    @Column(name = "utilisateur")
    private String utilisateur;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "cible")
    private String cible;

    @Column(name = "detail")
    private String detail;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Instant getDateHeure() { return dateHeure; }
    public void setDateHeure(Instant dateHeure) { this.dateHeure = dateHeure; }
    public String getUtilisateur() { return utilisateur; }
    public void setUtilisateur(String utilisateur) { this.utilisateur = utilisateur; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getCible() { return cible; }
    public void setCible(String cible) { this.cible = cible; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
