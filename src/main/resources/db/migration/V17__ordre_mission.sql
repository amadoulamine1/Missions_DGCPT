-- Ordre de mission (document PDF facultatif, un par mission)
CREATE TABLE ordre_mission (
    mission_id   INTEGER PRIMARY KEY REFERENCES mission(id) ON DELETE CASCADE,
    nom_fichier  VARCHAR(255) NOT NULL,
    type_mime    VARCHAR(100) NOT NULL DEFAULT 'application/pdf',
    taille       BIGINT       NOT NULL,
    contenu      BYTEA        NOT NULL,
    date_ajout   TIMESTAMP    NOT NULL DEFAULT now()
);
