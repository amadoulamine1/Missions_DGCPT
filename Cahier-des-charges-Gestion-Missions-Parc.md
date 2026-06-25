# Cahier des charges — Application de gestion des missions et du parc informatique

*Version 11 — cadrage initial enrichi des évolutions de réalisation (voir §9).*

## 1. Contexte et objectif

L'organisation est composée de **postes régionaux**. Chaque poste dispose d'un chef de poste, d'agents et d'un parc de matériel informatique. Des **missions** sont menées sur ces postes pour relever l'état du réseau et inventorier le matériel.

L'application doit permettre de :

- gérer les postes, leurs agents et leur matériel ;
- créer et suivre les missions ;
- saisir les relevés de mission (réseau + inventaire), en ligne ou hors-ligne ;
- **retrouver les résultats d'une mission**, **l'inventaire d'un poste**, et **l'inventaire de tout le parc à un moment donné**.

## 2. Principes structurants

Trois principes commandent toute la conception :

1. **Historisation.** Le chef de poste, comme les affectations de matériel, changent dans le temps. On conserve l'historique (avec dates de début et de fin), on n'écrase jamais.
2. **Photos datées.** Chaque relevé fait pendant une mission est figé comme une photo à sa date. C'est ce qui permet de reconstituer l'état du parc à n'importe quel moment.
3. **Mode hors-ligne natif.** À cause des coupures réseau, la saisie doit pouvoir se faire hors-ligne via des canevas Excel, puis être chargée et validée quand le réseau revient. Ce mode reste disponible en permanence, pas seulement au démarrage.

## 3. Modèle de données

### 3.1 Poste régional (TPR)
Entité de base, désignée **TPR** dans l'application. Possède un code, un nom/une région, une liste d'agents, un parc de matériel, et un chef de poste (historisé).
*(« poste de service » dans une mission = ce poste régional.)*

### 3.2 Agent
Personne (personnel de l'organisation) identifiée par son **matricule** (clé d'identité).

| Champ | Description |
|---|---|
| **Matricule** | Identifiant unique de l'agent |
| Nom | |
| Prénom | |
| Fonction | |
| Numéro de téléphone | |
| Adresse e-mail | |

Tous les agents partagent ces attributs, mais relèvent de **deux rattachements distincts** :

- **Agent de poste** — rattaché à **un seul poste régional à la fois** (rattachement **historisé** : un agent peut être **muté vers un autre TPR** avec date d'effet, l'historique des périodes étant conservé). Se voit attribuer du matériel et l'utilise ; peut être chef de poste. **N'effectue pas les missions.**
- **Agent informaticien** — rattaché à la **Direction Informatique**. **Effectue les missions** (chef de mission ou membre), réalise les relevés réseau et les inventaires.

Conséquence : le **chef de mission et les membres d'une mission sont toujours des agents informaticiens** ; le **chef de poste et l'attributaire d'une machine sont des agents de poste**. Un membre de mission n'est pas une fiche distincte : c'est un agent (informaticien) jouant un rôle dans une mission — même entité, même matricule.

### 3.3 Chef de poste *(historisé)*
Un poste a un chef de poste sur une **période** (date de début → date de fin). Il peut être remplacé à tout moment : on clôture la période en cours et on en ouvre une nouvelle. Le rôle « chef de service » mentionné en cours de cadrage est le **même** que chef de poste (un seul rôle).

Dans l'application, le chef de poste se **désigne ou se change directement depuis la page du TPR** (avec une date d'effet) : l'app clôt la période précédente et ouvre la nouvelle. À la création d'une mission, le **dernier chef connu du TPR est reconduit par défaut** et figé sur la mission.

### 3.4 Mission
Contient :

- l'**objet** de la mission ;
- le **chef de mission** (un agent informaticien, **désigné parmi les membres**) ;
- les **membres** de la mission (des agents informaticiens) ;
- la **période** (début → fin) ;
- le **poste** où elle se déroule ;
- le **chef de poste en fonction au moment de la mission**, figé dans la mission (même si le chef change plus tard, la mission garde le bon nom).

La mission porte aussi un **N° de mission** (référence générée à sa création et partagée par tous les fichiers du mode hors-ligne) et un **statut** : *en consolidation* tant que les fichiers des agents arrivent, *clôturée* une fois validée par le chef de mission.

Les **membres** sont choisis parmi les informaticiens (sélection multiple). **Règle :** un agent ne peut pas être membre de deux missions dont les **périodes se chevauchent** — l'application le bloque et signale la mission en conflit ; il faut d'abord l'en retirer.

### 3.5 Relevé de mission *(photo datée)*
Produit pendant la mission, rattaché au poste et à la mission, daté :

- **état du câblage réseau** ;
- **catégorie de câble** utilisée (référentiel) ;
- l'**inventaire du matériel** constaté.

Chaque ligne d'inventaire conserve l'**agent saisisseur** (qui l'a relevée), l'**agent traitant** (pour les ordinateurs) et, le cas échéant, la **zone/périmètre** couverte — utile quand plusieurs agents se partagent la mission. L'**agent traitant est historisé par mission** : ce n'est pas forcément le même informaticien qui traite une machine d'une mission à l'autre (voir §9.13).

### 3.6 Matériel
Identité commune à tous les types :

| Champ | Description |
|---|---|
| **Numéro d'inventaire** | Clé d'identité, **généré par l'application**. Étiquette physique. |
| Type | Ordinateur, imprimante, switch, access point, scanner de chèque, autre — **liste paramétrable** (voir §3.8 et §9.12) |
| Poste de rattachement | Historisé (le matériel peut changer de poste) |
| Nom | |
| Modèle | |
| Adresse(s) MAC | |
| Adresse IP | Le cas échéant |

Attributs spécifiques par type :

- **Ordinateur** : MAC ethernet, MAC wifi, nom de la machine, **RAM**, **processeur**, **disque dur**, **agent attributaire** (agent de poste), **agent traitant** (agent informaticien **membre de la mission** ; anciennement « agent installateur »), **logiciels installés** (Aster, Antivirus, SicCDD, CIC, Sysbudget — liste paramétrable).
- **Imprimante** : MAC, wifi, IP, nom, modèle, et toute information de paramétrage.
- **Switch / Access point** : attributs identiques — informations réseau (MAC, IP, nom, modèle…).
- **Scanner de chèque** : **numéro de série** (clé d'identification physique), **marque**, **modèle**. Pas d'adresse MAC.

### 3.7 Affectation de matériel *(historisée)*
Lien matériel ↔ agent (et/ou poste) avec période. Permet de savoir à qui/où était un matériel à une date donnée. Le matériel peut être **réaffecté à un autre agent** (du **même TPR** que le matériel) avec une **date d'effet** : la période courante est clôturée et une nouvelle ouverte, et l'**historique des propriétaires** est conservé et consultable sur la fiche de l'équipement.

**Distinction avec la mutation d'agent.** Réaffecter un matériel (matériel ↔ agent) et muter un agent (agent ↔ TPR, §3.2) sont **deux opérations indépendantes**, toutes deux historisées : quand un **agent change de TPR**, le **matériel reste dans son poste** — c'est l'agent qui se déplace, pas l'équipement.

### 3.8 Référentiels (paramétrables)
- **Logiciels** (extensible : on pourra en ajouter au-delà des 5 actuels) ;
- **Catégories de câble** ;
- **Types de matériel** — chaque type porte un **libellé** et un **préfixe** de n° d'inventaire ; les types ajoutés par l'administrateur enrichissent le parc et le canevas (voir §9.12).

### 3.9 Utilisateurs et rôles
- **Administrateur** : référentiels, postes, agents, comptes utilisateurs.
- **Chef de mission** : crée et gère ses missions, **valide les imports**.
- **Agent** : saisit les relevés (en ligne ou via canevas Excel).
- Les comptes des **chefs de mission** et des **agents** sont **rattachés à un agent informaticien** (un agent = un compte).

## 4. Règle d'identité du matériel et anti-doublon

Le **numéro d'inventaire est la clé**. Dans un canevas :

- si la machine **existe déjà**, l'agent **saisit son numéro d'inventaire** → l'app met à jour la fiche existante ;
- si la **case numéro est vide**, l'app considère que c'est un **nouveau matériel** et lui **attribue un numéro** au chargement.

Après validation, l'application restitue **la liste des matériels nouvellement créés avec leur numéro**, pour impression et pose des étiquettes. À la mission suivante, l'agent lit l'étiquette et reporte le numéro.

**Filet de sécurité** : si un matériel arrive sans numéro mais avec une **adresse MAC déjà connue** (ou, pour le scanner de chèque, un **numéro de série déjà connu**), le chef de mission est alerté (« ce matériel existe peut-être déjà ») avant validation.

## 5. Mode hors-ligne (canevas Excel)

Fonctionnement normal : saisie en ligne dans l'application. En cas de coupure :

1. **Génération** d'un canevas pré-rempli avec les référentiels du moment (agents du poste, logiciels, catégories de câble) sous forme de **listes déroulantes**, pour limiter les erreurs de saisie. Un jeu de canevas : en-tête de mission + relevé réseau, puis un onglet/fichier par type de matériel. Chaque ligne porte les identifiants du poste et de la mission.
2. **Saisie hors-ligne** par l'agent.
3. **Chargement** du canevas → création d'un **relevé en brouillon**.
4. **Contrôles** automatiques : format des adresses MAC, champs obligatoires, agent connu, détection de doublons.
5. **Validation par le chef de mission** : il voit le nouveau, le modifié et les erreurs, puis valide.
6. **Intégration** : la photo datée est figée, les fiches matériel créées/mises à jour, les numéros d'inventaire attribués aux nouveaux matériels.

### 5.1 Plusieurs agents, plusieurs fichiers (consolidation)

Sur une même mission, le chef de mission et les agents peuvent se répartir le travail et **remplir chacun leur propre fichier**. L'application les consolide grâce à trois mécanismes :

- **Référence de mission commune.** Le chef de mission crée d'abord la mission dans l'application (au siège, réseau disponible) ; l'app génère un **N° de mission**. Les canevas sont distribués déjà estampillés avec ce numéro et le code poste. À l'upload, chaque fichier se rattache automatiquement à la bonne mission.
- **Agent saisisseur par fichier.** L'en-tête de chaque fichier porte le **matricule de l'agent qui l'a rempli** ; cet agent doit être un **membre de la mission**. L'application trace ainsi, ligne par ligne, qui a saisi quoi.
- **Consolidation à l'import.** Tous les fichiers reçus alimentent le **relevé unique** de la mission. Le rapprochement par clé (N° d'inventaire, sinon MAC ou numéro de série) détecte les recouvrements entre agents : doublon signalé, et conflit à arbitrer par le chef si les valeurs diffèrent. La mission reste *en consolidation* jusqu'à sa **clôture par le chef de mission** ; les fichiers sont chargés au fur et à mesure qu'ils arrivent.

**Départage des tâches.** Pour éviter que deux agents saisissent le même matériel, le travail est réparti par **type de matériel** et/ou par **zone/périmètre** du poste (champ « zone » optionnel dans l'en-tête). Le **relevé réseau** (état du câblage, catégorie de câble) est renseigné **une seule fois** pour la mission, généralement par le chef.

### 5.2 Fiabilité de la saisie dans le canevas

Le canevas Excel intègre plusieurs garde-fous pour limiter les erreurs avant même l'import :

- **Listes déroulantes par rôle.** L'**attributaire** se choisit parmi les **agents du TPR** (feuille dédiée « Agents TPR », pré-remplie ; un agent absent de la base peut y être ajouté et sera **créé à l'import**). Le **saisisseur** et l'**agent traitant** se choisissent parmi les **membres de la mission**. Les chefs de mission et de poste sont **pré-remplis**. Les agents s'affichent au format « matricule — prénom nom ».
- **Indicateur d'état** (feuille « 1-Mission et Réseau ») : **CANEVAS CONFORME** (vert), **CANEVAS INCOMPLET** (rouge — un champ obligatoire manque) ou **CANEVAS NON CONFORME** (orange — un champ a un format invalide, p. ex. une adresse MAC).
- **Mise en forme conditionnelle** : un champ obligatoire laissé vide apparaît en **rouge** ; une adresse MAC mal formée apparaît en **orange**.
- **Validation de saisie des adresses MAC** : format `AA:BB:CC:DD:EE:FF` imposé, message d'aide à la sélection, refus des valeurs mal formées.
- **Protection de la mise en forme** : seules les cellules de saisie sont modifiables ; la mise en forme et les contrôles sont figés afin qu'un **collage** ne puisse pas les effacer (coller une valeur via « Collage spécial → Valeurs »).

## 6. Restitutions attendues

- **Résultats d'une mission** : relevé réseau + inventaire constaté, tels que figés à la date de la mission.
- **Inventaire d'un poste** : à la date du jour ou à une date passée.
- **Inventaire de tout le parc à un moment donné** : reconstitution à partir des photos datées et de l'historique des affectations.

## 7. Points à confirmer

Tous les points de cadrage métier sont tranchés à ce stade. Reste à définir en phase technique la politique de sauvegarde et de sécurité (voir §8) ; le volume du parc sera établi par l'inventaire lui-même.

## 8. Choix de plateforme et techniques

Décidés lors du cadrage :

- **Type d'application** : application **web centralisée**, consultée via navigateur, avec les **canevas Excel comme mode hors-ligne** intégré.
- **Hébergement** : **serveur interne (intranet)**.
- **Base de données** : **PostgreSQL** (gratuit, adapté à l'historisation et aux requêtes à une date).
- **Réalisation et maintenance** : équipe interne de la Direction Informatique.
- **Socle technique retenu** : **Java** — Spring Boot + Spring Data JPA (Hibernate) + Spring Security (gestion des rôles) + Thymeleaf (écrans) + Apache POI (canevas Excel).
- **Volumétrie** : ~20 postes régionaux ; ~2 000 agents au total (Trésor), dont ~700 en poste (≈ 35 par poste en moyenne), le reste au niveau central (dont la Direction Informatique). Le **parc n'est pas encore connu** — son inventaire est précisément l'un des objectifs de l'application ; estimé à l'ordre du millier d'équipements. Volume modéré : la stack retenue est largement dimensionnée, sans enjeu de performance.

À préciser en phase technique : le nombre de postes régionaux et le volume du parc matériel. La **politique de sauvegarde et de sécurité** fait l'objet d'un document dédié (« Politique-sauvegarde-securite.md »).


## 9. Évolutions et état de réalisation

Section ajoutée pendant le développement, en complément du cadrage initial.

### 9.1 Terminologie
- **TPR** = poste régional, terme employé dans toute l'application.

### 9.2 Cycle de vie d'une mission
- Le chef de mission **crée la mission en ligne** : choix du TPR (existant ou nouveau), objet, période, chef de mission, chef de poste et membres. L'application **génère le N° de mission** (format `MIS-AAAA-NNN`) puis un **canevas pré-estampillé** (en-tête déjà rempli) à distribuer aux agents.
- **Chef de poste** : reconduit par défaut (dernier chef connu du TPR), ou changé directement depuis la page du TPR avec date d'effet et historisation des périodes.
- **Membres** : choisis parmi les informaticiens (sélection multiple) ; **contrôle de chevauchement** (un agent ne peut pas être sur deux missions simultanées) ; retrait possible depuis le détail de la mission.
- **Chef de mission** : désigné **parmi les membres** de la mission (un et un seul).
- **Contrôle des dates** : la date de fin ne peut pas être antérieure à la date de début.
- **Édition d'une mission** après création : objet, dates, **statut** (en consolidation / clôturée), observations, **membres** et **chef de mission** modifiables (le N° de mission, le TPR et le chef de poste figé restent inchangés). Le contrôle de chevauchement **exclut la mission éditée**.

### 9.3 Référentiels et création à la volée
- Écrans dédiés de **gestion des TPR** (créer/modifier) et des **agents** (créer/modifier, type informaticien ou agent de poste).
- Pour fluidifier la saisie, les entités absentes (TPR, agents) peuvent être **créées à la volée** à la création d'une mission, à la désignation d'un chef ou à l'import. En production, ces créations implicites pourront être durcies en contrôles bloquants selon la politique d'administration.

### 9.4 Application réalisée
- **Web** : Spring Boot + Thymeleaf + Spring Data JPA + PostgreSQL (schéma appliqué par Flyway). Accesseurs Java explicites (sans Lombok) pour un build robuste sur tout JDK.
- **Écrans** : Accueil, Postes (TPR), Agents, Parc, Missions (liste, création, détail avec membres et relevés datés), Import (télécharger un canevas, aperçu, validation, intégration).
- **Import** : lecture du canevas (Apache POI), contrôles (formats, champs obligatoires), aperçu, **validation explicite** avant prise en compte, puis intégration en base (rapprochement, affectations historisées, relevés datés).
- **Configuration locale** non versionnée (`application-local.properties`) pour la connexion à la base.
- **Tests** : test d'intégration du pipeline d'import sur PostgreSQL (Testcontainers).

### 9.6 Canevas et matériel
- **Agent traitant** : l'ancien « agent installateur » de l'ordinateur est renommé **agent traitant** et restreint aux **membres de la mission**.
- **Attributaire** : choisi parmi les **agents du TPR** via la feuille « Agents TPR » (pré-remplie et complétable) ; les agents manquants sont **créés à l'import**.
- **Caractéristiques d'ordinateur** ajoutées : **RAM**, **processeur**, **disque dur**.
- **Garde-fous du canevas** : indicateur conforme/incomplet/non conforme, mises en forme conditionnelles (obligatoires en rouge, MAC invalide en orange), validation du format des MAC, et protection de la mise en forme contre le collage (voir §5.2).
- **Pré-remplissage de l'inventaire** : au téléchargement du canevas pré-rempli, le matériel déjà connu du poste est reporté dans les feuilles (caractéristiques, statut, affectataire, logiciels) ; l'agent vérifie, corrige et ajoute les nouveautés.

### 9.7 Authentification, rôles et comptes
- **Connexion par formulaire** (Spring Security) ; mots de passe **chiffrés en BCrypt** ; déconnexion.
- **Trois rôles** : **Administrateur** (gestion des postes, agents, comptes, référentiels), **Chef de mission** (création et **édition** de mission, validation des imports), **Agent** (consultation, téléchargement des canevas, téléversement des fichiers).
- **Règles d'accès** appliquées par rôle et **navigation adaptée** (liens et actions réservés masqués).
- **Consultation des postes et de leur inventaire** ouverte à tous les rôles ; la **création/modification** des postes et des agents reste réservée à l'administrateur.
- **Gestion des comptes** par l'administrateur : créer, modifier, désactiver, **réinitialiser** le mot de passe (valeur temporaire) ; le mot de passe est **affichable** à la saisie.
- **Comptes liés aux agents informaticiens** : un compte (chef de mission / agent) est rattaché à un agent informaticien (un agent = un compte) ; dans ce cas l'**identifiant devient le matricule** et le **nom est repris** de l'agent.
- **Compte administrateur initial** créé au premier démarrage (`admin` / `admin`, à changer aussitôt).
- *Note technique* : la protection CSRF est désactivée pour l'intranet ; à réactiver pour une mise en production exposée.

### 9.8 Statut, observations et suivi
- **« Affecté à »** : intitulé retenu pour l'agent attributaire d'un ordinateur (plus parlant pour les agents).
- **Statut du matériel** : **En service / En panne / À changer**, saisi dans le canevas et affiché (parc, fiche poste, fiche équipement).
- **Observations** : champ libre par matériel, et une observation générale par mission.
- **Statut temporel de la mission** dérivé des dates : **Planifiée** (à venir), **En cours**, **Terminée**.
- **Fiche détaillée d'un équipement** : caractéristiques par type (MAC, RAM, processeur, disque, logiciels, n° série…), statut, observations, **affectation courante**, **historique des propriétaires** et **historique des relevés**.
- **Page agents** : présentation en **fiches (cartes)** séparées (**informaticiens** / **agents de poste**), avec **recherche** et **filtre par TPR**.
- **Réaffectation du matériel** : depuis la fiche d'un équipement (réservé administrateur), réaffectation à un autre agent **du même TPR**, avec **date d'effet** ; **historique des propriétaires** affiché (agent, période). Le contrôle « même poste » est appliqué dans la liste de choix **et** côté serveur.
- **Mutation d'un agent** : depuis la fiche d'un agent de poste (réservé administrateur), changement de **TPR de rattachement** avec **date d'effet** ; **historique des rattachements** affiché. Conformément au §3.7, **le matériel n'est pas déplacé**.

### 9.9 Consolidation multi-fichiers et arbitrage des conflits
- Chaque canevas chargé devient un **lot** rattaché à sa mission (stocké, contrôlé à l'upload) ; plusieurs lots s'accumulent pour une même mission.
- Une **page de consolidation** par mission liste les lots en attente et **détecte les conflits** : un même matériel (clé = n° d'inventaire, sinon MAC / n° de série) saisi avec des **valeurs divergentes** entre deux fichiers.
- Le **chef de mission arbitre** chaque conflit (choix de la version à retenir), puis **intègre l'ensemble** en une transaction (rapprochement par clé, affectations historisées, relevés datés) ; les lots passent à *intégré*. Un lot erroné peut être **retiré** avant intégration.

### 9.10 Restitutions, recherche et exports
- **Tableau de bord** (accueil) : chiffres clés (postes, matériel, **taux de disponibilité**, **postes en alerte**, missions, agents) et **graphiques en barres** (matériel par statut, par type, **par poste**, et missions par état). **Exports des statistiques** : **Excel** (classeur synthèse / par type / par poste) et **PDF** (mise en page d'impression).
- **Inventaire à une date** : reconstitution de la **composition et de la localisation** du parc à une date donnée, à partir de l'historique des affectations.
- **Parc** : recherche (n°, nom, modèle), filtres rapides (**type** et **statut** en pastilles, poste), **tri par colonnes** et **pagination** ; pour les **ordinateurs**, affichage de la **RAM, du processeur et du disque** ; **export Excel** de l'inventaire filtré.
- **Missions** : recherche (n°, objet), filtres (poste, état), tri et pagination, avec **export Excel des relevés** d'une mission.
- **Agents** : présentation en **fiches (cartes)** séparées (**informaticiens** / **agents de poste**), avec **recherche** et **filtre par TPR** pour les agents de poste ; listes paginées.
- **Étiquettes** : page imprimable des **n° d'inventaire** (filtrable comme le parc), à imprimer ou enregistrer en **PDF** pour étiqueter physiquement les machines.

### 9.11 Interface et navigation
- **Menu latéral** (à gauche) : Accueil, Postes, Agents, Parc, Missions, Importer, Guide, Comptes — avec mise en évidence de la page courante, repli en barre horizontale sur petit écran et masquage à l'impression.
- **Thème institutionnel** : fond clair neutre, accent **or** (DGCPT), typographie sans-serif ; tableaux à en-tête doré, **pastilles de statut** et **filtres en pastilles**.
- **Présentation adaptée** : tableaux triables et paginés pour les listes volumineuses (parc, missions, postes), **fiches** pour les agents, indicateurs et graphiques pour le tableau de bord.

### 9.12 Types de matériel paramétrables
- Les **types de matériel** deviennent un **référentiel** administrable (écran *Référentiels*) : l'administrateur ajoute un type avec son **libellé** et son **préfixe** de n° d'inventaire (2 à 4 lettres, unique). Les six types historiques (Ordinateur, Imprimante, Switch, Access point, Scanner chèque, Autre) sont **système** : non modifiables et non supprimables.
- Conception : l'application conserve une **famille technique** (le comportement câblé des types riches — sous-type, feuille de caractéristiques, regroupement réseau — reste inchangé) ; les types ajoutés sont des matériels **génériques** de famille « Autre », limités aux **attributs communs** (nom, modèle, MAC, IP, statut, observation).
- **Canevas** : un onglet **« 7-Autres matériels »** accueille ces types via une **liste déroulante** alimentée par le référentiel. À l'import, le **préfixe** du type alimente le numéro d'inventaire (`CODEPOSTE-PRÉFIXE-SÉQUENCE`) ; un type inconnu est signalé comme anomalie bloquante.
- **Observation** : le champ d'observation du matériel, déjà saisi dans le canevas, est désormais **affiché** sur la fiche du poste (colonne) et en infobulle dans le parc.

### 9.13 Historisations, obligations de saisie et canevas par agent
- **Agent traitant historisé par mission** : l'agent traitant est porté par le **relevé** (et non plus seulement par la machine). Le canevas d'une nouvelle mission charge les machines du poste **sans reporter l'agent traitant** (champ vide, à ressaisir) ; l'ordinateur conserve le « dernier agent traitant » pour l'affichage, et la fiche équipement montre l'historique par mission.
- **Fiche poste enrichie** : en plus du chef, des agents et du matériel, elle présente les **historiques** du poste — missions menées, fichiers (canevas) chargés, affectations de matériel et chefs de poste successifs.
- **Obligations de saisie renforcées** : le **statut** du matériel devient obligatoire (à l'import et signalé en rouge dans le canevas) ; l'**état du câblage** (Neuf / Bon / Pas bon) et la **catégorie de câble** sont obligatoires ; le relevé réseau saisi dans le canevas est **reporté sur la mission** à l'import.
- **Canevas par agent** : le téléchargement produit **un canevas par agent membre** (regroupés en ZIP), chacun nommé d'après l'agent et avec son matricule pré-renseigné comme agent saisisseur. Onglets réordonnés (Agents TPR juste après l'en-tête, « 7-Autres matériels » avant les référentiels) ; l'onglet générique est mis en forme (en-têtes, listes, surlignage des champs manquants).
- **Restitutions** : les relevés d'une mission affichent l'agent saisisseur en « matricule — prénom nom » et le statut ; la liste des missions est triée du plus récent par défaut.
- **Refonte de l'interface** : tableau de bord repensé (anneau de disponibilité, KPI), **identité éditoriale** (titres serif, filet doré) déployée sur toutes les pages, parc en mode large, et améliorations d'**accessibilité** (contrastes, focus clavier, repères ARIA).

### 9.5 Pistes d'évolution
Les chantiers structurants du cadrage sont réalisés (authentification et rôles, consolidation et arbitrage des conflits, inventaire à une date, restitutions et exports). Évolutions possibles ultérieurement :
- **Snapshot exact des attributs** à une date passée (au-delà de la composition/localisation), en figeant la photo `etat_observe` à chaque intégration.
- Affinements ergonomiques (notifications, recherche avancée, exports complémentaires).
- Mise en œuvre opérationnelle de la **politique de sauvegarde et de sécurité** (document dédié).
