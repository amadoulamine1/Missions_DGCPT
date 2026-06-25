package sn.dgcpt.missionsparc.mission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PosteForm {
    private Integer id;

    @NotBlank(message = "Le code est obligatoire.")
    @Size(max = 30, message = "Le code ne doit pas dépasser 30 caractères.")
    private String code;

    @NotBlank(message = "Le nom est obligatoire.")
    @Size(max = 150, message = "Le nom ne doit pas dépasser 150 caractères.")
    private String nom;

    @Size(max = 100, message = "La région ne doit pas dépasser 100 caractères.")
    private String region;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
}
