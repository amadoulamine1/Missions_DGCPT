package sn.dgcpt.missionsparc.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "agent")
public class Agent {

    @Id
    private String matricule;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    private String fonction;
    private String telephone;
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_agent", nullable = false)
    private TypeAgent typeAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poste_id")
    private Poste poste;

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getFonction() { return fonction; }
    public void setFonction(String fonction) { this.fonction = fonction; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public TypeAgent getTypeAgent() { return typeAgent; }
    public void setTypeAgent(TypeAgent typeAgent) { this.typeAgent = typeAgent; }
    public Poste getPoste() { return poste; }
    public void setPoste(Poste poste) { this.poste = poste; }
}
