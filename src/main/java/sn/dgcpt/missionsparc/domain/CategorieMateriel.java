package sn.dgcpt.missionsparc.domain;

import jakarta.persistence.*;

/**
 * Type de matériel paramétrable (cahier §3.8), affiché « Type » dans l'IHM.
 * Le {@link TypeMateriel} reste la « famille technique » qui pilote le comportement câblé
 * (sous-type, onglet du canevas, regroupement réseau). Une catégorie porte le libellé
 * affiché et le préfixe du n° d'inventaire ; les types créés par l'administrateur sont de
 * famille {@code AUTRE} (attributs communs uniquement).
 */
@Entity
@Table(name = "categorie_materiel")
public class CategorieMateriel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String libelle;

    @Column(nullable = false, unique = true, length = 4)
    private String prefixe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeMateriel famille = TypeMateriel.AUTRE;

    @Column(nullable = false)
    private boolean actif = true;

    /** Vrai pour les 6 types câblés (seedés) : non renommables ni supprimables. */
    @Column(nullable = false)
    private boolean systeme = false;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
    public String getPrefixe() { return prefixe; }
    public void setPrefixe(String prefixe) { this.prefixe = prefixe; }
    public TypeMateriel getFamille() { return famille; }
    public void setFamille(TypeMateriel famille) { this.famille = famille; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
    public boolean isSysteme() { return systeme; }
    public void setSysteme(boolean systeme) { this.systeme = systeme; }
}
