package sn.dgcpt.missionsparc.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "categorie_cable")
public class CategorieCable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String libelle;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
}
