-- Photo datée : snapshot des attributs observés du matériel au moment du relevé.
ALTER TABLE releve_materiel ADD COLUMN IF NOT EXISTS etat_observe TEXT;
