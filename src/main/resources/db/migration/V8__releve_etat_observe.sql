-- Photo datée : snapshot des attributs observés du matériel au moment du relevé.
-- Idempotent : la colonne a pu être créée par un démarrage antérieur (Hibernate/auto-ddl).
ALTER TABLE releve_materiel ADD COLUMN IF NOT EXISTS etat_observe TEXT;
