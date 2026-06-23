package sn.dgcpt.missionsparc.importation.dto;

import lombok.Getter;
import lombok.Setter;

/** Switch ou Access point (onglet "5-Switchs et AP"). */
@Getter
@Setter
public class LigneEquipementReseau {
    private int numLigne;
    private String numeroInventaire;
    private String type;   // "Switch" ou "Access point"
    private String nom;
    private String modele;
    private String mac;
    private String ip;
}
