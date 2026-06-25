package sn.dgcpt.missionsparc.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "utilisateur")
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleUtilisateur role;

    @Column(nullable = false)
    private boolean actif = true;

    /** Force la redirection vers la page de changement tant que le mot de passe par défaut/temporaire n'est pas modifié. */
    @Column(name = "mot_de_passe_a_changer", nullable = false)
    private boolean motDePasseAChanger = false;

    @Column(name = "nom_complet")
    private String nomComplet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_matricule")
    private Agent agent;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }
    public RoleUtilisateur getRole() { return role; }
    public void setRole(RoleUtilisateur role) { this.role = role; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
    public boolean isMotDePasseAChanger() { return motDePasseAChanger; }
    public void setMotDePasseAChanger(boolean motDePasseAChanger) { this.motDePasseAChanger = motDePasseAChanger; }
    public String getNomComplet() { return nomComplet; }
    public void setNomComplet(String nomComplet) { this.nomComplet = nomComplet; }
    public Agent getAgent() { return agent; }
    public void setAgent(Agent agent) { this.agent = agent; }
}
