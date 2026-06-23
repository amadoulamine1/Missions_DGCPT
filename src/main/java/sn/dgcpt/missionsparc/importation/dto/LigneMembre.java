package sn.dgcpt.missionsparc.importation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LigneMembre {
    private int numLigne;
    private String matricule;
    private String nom;
    private String prenom;
    private String fonction;
    private String telephone;
    private String email;
}
