-- Correctif de type : la colonne etat_observe avait été déclarée JSONB en V1, mais l'implémentation
-- y stocke un RÉSUMÉ TEXTUEL des attributs observés (cf. IntegrationService.construireEtatObserve)
-- et l'entité ReleveMateriel la mappe en TEXT. L'écriture d'une chaîne dans une colonne jsonb échoue
-- (« colonne etat_observe est de type jsonb mais l'expression est de type character varying »).
-- On aligne donc le type sur TEXT. La conversion ::text est neutre si la colonne est déjà TEXT.
ALTER TABLE releve_materiel ALTER COLUMN etat_observe TYPE TEXT USING etat_observe::text;
