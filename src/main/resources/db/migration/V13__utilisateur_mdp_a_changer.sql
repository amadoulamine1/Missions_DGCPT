-- Sécurité : forçage du changement de mot de passe (compte initial admin/admin, réinitialisations).
-- L'utilisateur portant ce drapeau est redirigé vers la page de changement tant qu'il ne l'a pas modifié.
ALTER TABLE utilisateur
    ADD COLUMN IF NOT EXISTS mot_de_passe_a_changer BOOLEAN NOT NULL DEFAULT FALSE;
