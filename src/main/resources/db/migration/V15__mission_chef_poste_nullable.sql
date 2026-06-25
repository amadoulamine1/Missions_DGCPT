-- Le chef de poste d'une mission peut être inconnu à la création (il est alors renseigné plus tard,
-- via le canevas, à l'import). On lève donc la contrainte NOT NULL sur la colonne correspondante.
ALTER TABLE mission ALTER COLUMN chef_poste_fige_matricule DROP NOT NULL;
