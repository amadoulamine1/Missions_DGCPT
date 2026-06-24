-- Caractéristiques matérielles de l'ordinateur (ajout)
ALTER TABLE ordinateur ADD COLUMN ram        VARCHAR(80);
ALTER TABLE ordinateur ADD COLUMN processeur VARCHAR(120);
ALTER TABLE ordinateur ADD COLUMN disque_dur VARCHAR(80);
