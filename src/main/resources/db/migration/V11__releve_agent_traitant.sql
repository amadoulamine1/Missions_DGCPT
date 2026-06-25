-- Historisation de l'agent traitant PAR MISSION : ce n'est pas forcément le même informaticien qui
-- traite la même machine d'une mission à l'autre. L'agent traitant est désormais porté par le relevé
-- (photo datée), en plus du « dernier agent traitant » conservé sur l'ordinateur pour affichage.
ALTER TABLE releve_materiel
    ADD COLUMN IF NOT EXISTS agent_traitant_matricule VARCHAR(30) REFERENCES agent(matricule);
