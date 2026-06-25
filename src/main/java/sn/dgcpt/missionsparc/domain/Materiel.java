package sn.dgcpt.missionsparc.domain;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Équipement du parc, identifié par son numéro d'inventaire.
 *
 * <p><b>Redondance maîtrisée {@link #type} ↔ {@link #categorie}.</b> Le champ {@code type}
 * est la <i>famille technique</i> (enum {@link TypeMateriel}) : il est figé dans le code et
 * pilote les comportements câblés (sous-entités dédiées, onglets du canevas, regroupement
 * réseau, {@code switch} exhaustifs de {@code ConsultationService}, {@code IntegrationService},
 * {@code CanevasWriter}, {@code StatsExporter}). Le champ {@code categorie} est le <i>type
 * paramétrable</i> ({@link CategorieMateriel} : libellé affiché + préfixe d'inventaire), que
 * l'administrateur gère dans les référentiels. <b>Invariant :</b> {@code categorie.famille == type},
 * garanti à l'intégration ({@code IntegrationService}) — la famille est dérivée de la catégorie,
 * jamais saisie de façon indépendante. Cette duplication est volontaire : elle évite de dynamiser
 * les {@code switch} métier tout en rendant le libellé/préfixe paramétrable (cf. cahier §3.8).
 *
 * <p>{@link #mac}/{@link #ip} ne sont renseignés que pour la famille générique {@code AUTRE} ;
 * les familles câblées portent ces informations dans leur sous-entité dédiée.
 */
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
