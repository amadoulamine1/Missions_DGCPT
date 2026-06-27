package sn.dgcpt.missionsparc.domain;

import jakarta.persistence.*;

import java.time.Instant;

/** Ordre de mission au format PDF, facultatif ; une mission peut en porter plusieurs. */
@Entity
@Table(name = "ordre_mission")
public class OrdreMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "mission_id", nullable = false)
    private Integer missionId;

    @Column(name = "nom_fichier", nullable = false)
    private String nomFichier;

    @Column(name = "type_mime", nullable = false)
    private String typeMime;

    @Column(name = "taille", nullable = false)
    private long taille;

    @Column(name = "contenu", nullable = false)
    private byte[] contenu;

    @Column(name = "date_ajout", nullable = false)
    private Instant dateAjout;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getMissionId() { return missionId; }
    public void setMissionId(Integer missionId) { this.missionId = missionId; }
    public String getNomFichier() { return nomFichier; }
    public void setNomFichier(String nomFichier) { this.nomFichier = nomFichier; }
    public String getTypeMime() { return typeMime; }
    public void setTypeMime(String typeMime) { this.typeMime = typeMime; }
    public long getTaille() { return taille; }
    public void setTaille(long taille) { this.taille = taille; }
    public byte[] getContenu() { return contenu; }
    public void setContenu(byte[] contenu) { this.contenu = contenu; }
    public Instant getDateAjout() { return dateAjout; }
    public void setDateAjout(Instant dateAjout) { this.dateAjout = dateAjout; }
}
