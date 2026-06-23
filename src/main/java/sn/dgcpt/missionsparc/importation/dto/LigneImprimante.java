package sn.dgcpt.missionsparc.importation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LigneImprimante {
    private int numLigne;
    private String numeroInventaire;
    private String nom;
    private String modele;
    private String mac;
    private String macWifi;
    private String ip;
}
