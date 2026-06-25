package sn.dgcpt.missionsparc.agent;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AgentForm {
    @NotBlank(message = "Le matricule est obligatoire.")
    @Size(max = 30, message = "Le matricule ne doit pas dépasser 30 caractères.")
    private String matricule;

    @NotBlank(message = "Le nom est obligatoire.")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères.")
    private String nom;

    @Size(max = 100, message = "Le prénom ne doit pas dépasser 100 caractères.")
    private String prenom;

    @Size(max = 150, message = "La fonction ne doit pas dépasser 150 caractères.")
    private String fonction;

    @Size(max = 30, message = "Le téléphone ne doit pas dépasser 30 caractères.")
    private String telephone;

    @Email(message = "L'adresse e-mail n'est pas valide.")
    @Size(max = 150, message = "L'e-mail ne doit pas dépasser 150 caractères.")
    private String email;

    private String typeAgent = "POSTE";   // POSTE | INFORMATICIEN
    private Integer posteId;

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
    public String getTypeAgent() { return typeAgent; }
    public void setTypeAgent(String typeAgent) { this.typeAgent = typeAgent; }
    public Integer getPosteId() { return posteId; }
    public void setPosteId(Integer posteId) { this.posteId = posteId; }
}
