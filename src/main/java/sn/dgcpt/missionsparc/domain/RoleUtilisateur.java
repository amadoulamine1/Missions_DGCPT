package sn.dgcpt.missionsparc.domain;

/**
 * Rôles applicatifs. {@code MANAGER} : profil de pilotage en lecture seule — il consulte toutes les
 * restitutions (tableau de bord, postes, parc, missions, agents, rapport annuel) pour décider, sans
 * pouvoir modifier les données.
 */
public enum RoleUtilisateur { ADMIN, CHEF_MISSION, AGENT, MANAGER }
