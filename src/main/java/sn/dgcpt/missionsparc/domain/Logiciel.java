package sn.dgcpt.missionsparc.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "logiciel")
public class Logiciel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String nom;

    @Column(nullable = false)
    private boolean actif = true;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
}
