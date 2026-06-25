-- Historique des statuts : on conserve le statut du matériel observé À CHAQUE RELEVÉ (par mission),
-- en plus du statut courant porté par la fiche matériel. Permet de retracer l'évolution
-- En service / En panne / À changer dans le temps via les relevés.
ALTER TABLE releve_materiel
    ADD COLUMN IF NOT EXISTS statut_observe VARCHAR(15)
        CHECK (statut_observe IS NULL OR statut_observe IN ('EN_SERVICE', 'EN_PANNE', 'A_CHANGER'));
