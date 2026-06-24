-- Lien optionnel entre un compte utilisateur et un agent informaticien (un agent = un compte)
ALTER TABLE utilisateur ADD COLUMN agent_matricule VARCHAR(30) REFERENCES agent(matricule);
CREATE UNIQUE INDEX uq_utilisateur_agent ON utilisateur(agent_matricule) WHERE agent_matricule IS NOT NULL;
