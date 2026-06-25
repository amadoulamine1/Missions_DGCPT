-- Logiciel « AD » (Active Directory) ajouté à la liste de référence des logiciels installables
-- sur les ordinateurs. Idempotent : sans effet si « AD » existe déjà (saisi via le référentiel).
INSERT INTO logiciel (nom)
SELECT 'AD'
WHERE NOT EXISTS (SELECT 1 FROM logiciel WHERE nom = 'AD');
