package sn.dgcpt.missionsparc.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "materiel")
public class Materiel {

    @Id
    @Column(name = "numero_inventaire")
    private String numeroInventaire;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeMateriel type;

    private String nom;
    private String modele;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutMateriel statut;

    @Column(name = "observation", length = 2000)
    private String observation;

    /** MAC/IP portées directement par le matériel pour les types génériques (famille AUTRE) ;
     *  les types câblés stockent ces informations dans leur sous-entité dédiée. */
    @Column(name = "mac")
    private String mac;

    @Column(name = "ip")
    private String ip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poste_id")
    private Poste poste;

    /** Type paramétrable affiché (libellé + préfixe). La famille technique reste {@link #type}. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categorie_id")
    private CategorieMateriel categorie;

    @Column(name = "date_creation")
    private Instant dateCreation;

    public String getNumeroInventaire() { return numeroInventaire; }
    public void setNumeroInventaire(String numeroInventaire) { this.numeroInventaire = numeroInventaire; }
    public TypeMateriel getType() { return type; }
    public void setType(TypeMateriel type) { this.type = type; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getModele() { return modele; }
    public void setModele(String modele) { this.modele = modele; }
    public Poste getPoste() { return poste; }
    public void setPoste(Poste poste) { this.poste = poste; }
    public Instant getDateCreation() { return dateCreation; }
    public void setDateCreation(Instant dateCreation) { this.dateCreation = dateCreation; }
    public StatutMateriel getStatut() { return statut; }
    public void setStatut(StatutMateriel statut) { this.statut = statut; }
    public String getObservation() { return observation; }
    public void setObservation(String observation) { this.observation = observation; }
    public CategorieMateriel getCategorie() { return categorie; }
    public void setCategorie(CategorieMateriel categorie) { this.categorie = categorie; }
    public String getMac() { return mac; }
    public void setMac(String mac) { this.mac = mac; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
}
