-- Statut et observation du matériel ; observations de la mission
ALTER TABLE materiel ADD COLUMN statut       VARCHAR(20);
ALTER TABLE materiel ADD COLUMN observation  VARCHAR(2000);
ALTER TABLE mission  ADD COLUMN observations VARCHAR(2000);
