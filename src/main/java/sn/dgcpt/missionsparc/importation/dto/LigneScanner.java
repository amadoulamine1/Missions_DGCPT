package sn.dgcpt.missionsparc.importation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LigneScanner {
    private int numLigne;
    private String numeroInventaire;
    private String numeroSerie;
    private String marque;
    private String modele;
}
