# Cahier des charges — Application de gestion des missions et du parc informatique

*Document de travail — version 1 (récapitulatif de cadrage)*

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

### 3.1 Poste régional
Entité de base. Possède un code, un nom/une région, une liste d'agents, un parc de matériel, et un chef de poste (historisé).
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

- **Agent de poste** — rattaché à **un seul poste régional à la fois** (rattachement qui peut évoluer dans le temps). Se voit attribuer du matériel et l'utilise ; peut être chef de poste. **N'effectue pas les missions.**
- **Agent informaticien** — rattaché à la **Direction Informatique**. **Effectue les missions** (chef de mission ou membre), réalise les relevés réseau et les inventaires.

Conséquence : le **chef de mission et les membres d'une mission sont toujours des agents informaticiens** ; le **chef de poste et l'attributaire d'une machine sont des agents de poste**. Un membre de mission n'est pas une fiche distincte : c'est un agent (informaticien) jouant un rôle dans une mission — même entité, même matricule.

### 3.3 Chef de poste *(historisé)*
Un poste a un chef de poste sur une **période** (date de début → date de fin). Il peut être remplacé à tout moment : on clôture la période en cours et on en ouvre une nouvelle. Le rôle « chef de service » mentionné en cours de cadrage est le **même** que chef de poste (un seul rôle).

### 3.4 Mission
Contient :

- l'**objet** de la mission ;
- le **chef de mission** (un agent informaticien) ;
- les **membres** de la mission (des agents informaticiens) ;
- la **période** (début → fin) ;
- le **poste** où elle se déroule ;
- le **chef de poste en fonction au moment de la mission**, figé dans la mission (même si le chef change plus tard, la mission garde le bon nom).

La mission porte aussi un **N° de mission** (référence générée à sa création et partagée par tous les fichiers du mode hors-ligne) et un **statut** : *en consolidation* tant que les fichiers des agents arrivent, *clôturée* une fois validée par le chef de mission.

### 3.5 Relevé de mission *(photo datée)*
Produit pendant la mission, rattaché au poste et à la mission, daté :

- **état du câblage réseau** ;
- **catégorie de câble** utilisée (référentiel) ;
- l'**inventaire du matériel** constaté.

Chaque ligne d'inventaire conserve l'**agent saisisseur** (qui l'a relevée) et, le cas échéant, la **zone/périmètre** couverte — utile quand plusieurs agents se partagent la mission.

### 3.6 Matériel
Identité commune à tous les types :

| Champ | Description |
|---|---|
| **Numéro d'inventaire** | Clé d'identité, **généré par l'application**. Étiquette physique. |
| Type | Ordinateur, imprimante, switch, access point, scanner de chèque, autre |
| Poste de rattachement | Historisé (le matériel peut changer de poste) |
| Nom | |
| Modèle | |
| Adresse(s) MAC | |
| Adresse IP | Le cas échéant |

Attributs spécifiques par type :

- **Ordinateur** : MAC ethernet, MAC wifi, nom de la machine, **agent attributaire** (agent de poste), **agent installateur** (agent informaticien), **logiciels installés** (Aster, Antivirus, SicCDD, CIC, Sysbudget — liste paramétrable).
- **Imprimante** : MAC, wifi, IP, nom, modèle, et toute information de paramétrage.
- **Switch / Access point** : attributs identiques — informations réseau (MAC, IP, nom, modèle…).
- **Scanner de chèque** : **numéro de série** (clé d'identification physique), **marque**, **modèle**. Pas d'adresse MAC.

### 3.7 Affectation de matériel *(historisée)*
Lien matériel ↔ agent (et/ou poste) avec période. Permet de savoir à qui/où était un matériel à une date donnée.

### 3.8 Référentiels (paramétrables)
- **Logiciels** (extensible : on pourra en ajouter au-delà des 5 actuels) ;
- **Catégories de câble** ;
- **Types de matériel**.

### 3.9 Utilisateurs et rôles
- **Administrateur** : référentiels, postes, agents, comptes utilisateurs.
- **Chef de mission** : crée et gère ses missions, **valide les imports**.
- **Agent** : saisit les relevés (en ligne ou via canevas Excel).

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
- **Agent saisisseur par fichier.** L'en-tête de chaque fichier porte le **matricule de l'agent qui l'a rempli**. L'application trace ainsi, ligne par ligne, qui a saisi quoi.
- **Consolidation à l'import.** Tous les fichiers reçus alimentent le **relevé unique** de la mission. Le rapprochement par clé (N° d'inventaire, sinon MAC ou numéro de série) détecte les recouvrements entre agents : doublon signalé, et conflit à arbitrer par le chef si les valeurs diffèrent. La mission reste *en consolidation* jusqu'à sa **clôture par le chef de mission** ; les fichiers sont chargés au fur et à mesure qu'ils arrivent.

**Départage des tâches.** Pour éviter que deux agents saisissent le même matériel, le travail est réparti par **type de matériel** et/ou par **zone/périmètre** du poste (champ « zone » optionnel dans l'en-tête). Le **relevé réseau** (état du câblage, catégorie de câble) est renseigné **une seule fois** pour la mission, généralement par le chef.

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

À préciser en phase technique : le nombre de postes régionaux et le volume du parc matériel ; la politique de sauvegarde et de sécurité.
