package sn.dgcpt.missionsparc.importation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LigneOrdinateur {
    private int numLigne;
    private String numeroInventaire;
    private String nomMachine;
    private String modele;
    private String macEthernet;
    private String macWifi;
    private String agentAttributaire;
    private String agentInstallateur;
    private boolean aster;
    private boolean antivirus;
    private boolean sicCDD;
    private boolean cic;
    private boolean sysbudget;
}
