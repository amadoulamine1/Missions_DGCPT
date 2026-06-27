-- Nouveau rôle MANAGER (profil de pilotage, lecture seule). La colonne utilisateur.role porte une
-- contrainte CHECK (V4) qu'il faut étendre pour accepter 'MANAGER'.
ALTER TABLE utilisateur DROP CONSTRAINT IF EXISTS utilisateur_role_check;
ALTER TABLE utilisateur ADD CONSTRAINT utilisateur_role_check
    CHECK (role IN ('ADMIN', 'CHEF_MISSION', 'AGENT', 'MANAGER'));
