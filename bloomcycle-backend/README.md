# BloomCycle

BloomCycle est une plateforme conçue pour gérer, déployer, et maintenir en condition opérationnelle des applications. Ce projet est construit avec **Spring Boot** et se concentre sur le déploiement continu et la partie "Run" de l'infrastructure.

## Fonctionnalités

### Gestion des Applications
- **Déploiement Continu** :
  - Import depuis Git ou archive ZIP
  - Support multi-technologies (Java, Node.js, Python)
  - Génération automatique des Dockerfiles
  - Déploiement via conteneurs Docker

- **Gestion des Conteneurs** :
  - Démarrage, arrêt et redémarrage des applications
  - Support du multi-instances
  - Redémarrage automatique en cas de crash
  - Rolling updates sans interruption de service

### Monitoring & Configuration
- **Surveillance en Temps Réel** :
  - Métriques système (CPU, mémoire, réseau)
  - État des conteneurs (en cours, arrêté, crashé)
  - Logs applicatifs centralisés

- **Configuration Flexible** :
  - Variables d'environnement
  - Fichiers de configuration
  - Gestion des secrets

### Sécurité & API
- **Sécurité Intégrée** :
  - Authentification JWT
  - Gestion fine des droits d'accès
  - Isolation des conteneurs
  - HTTPS/TLS

- **API REST Complète** :
  - Documentation OpenAPI/Swagger
  - Gestion des projets et conteneurs
  - Monitoring et métriques
  - Webhooks pour les événements

### Traefik

> En local, **Traefik** redirige correctement les ports vers les projets déployés.  
> Cependant, la redirection automatique ne fonctionne pas encore avec des **domaines personnalisés** (ex. `mon-app.mondomaine.com`). Cette fonctionnalité est en cours de développement.

## 🛠 Prérequis

- Java 23+
- Maven
- Docker
- PostgreSQL
- Git

## Configuration

### Variables d'Environnement

```properties
# Base de données
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bloomcycle
SPRING_DATASOURCE_USERNAME=votre_username
SPRING_DATASOURCE_PASSWORD=votre_password

# JWT
JWT_SECRET=votre_secret_jwt

# Stockage
APP_STORAGE_PATH=/chemin/vers/stockage
```

## Installation

1. Cloner le projet :
```bash
git clone https://github.com/axelfrache/bloomcycle.git
cd bloomcycle/bloomcycle-backend
```

2. Compiler le projet :
```bash
mvn clean install
```

3. Lancer l'application :
```bash
mvn spring-boot:run
```

## Structure du Projet

```
src/main/java/fr/umontpellier/bloomcycle/
├── controller/         # Contrôleurs REST
├── dto/               # Objets de transfert de données
├── model/             # Entités JPA
├── repository/        # Repositories Spring Data
├── security/          # Configuration sécurité et JWT
└── service/          # Logique métier
```

## API REST

### Documentation Swagger

La documentation complète de l'API est disponible via Swagger UI à l'adresse :
```
http://localhost:9090/swagger-ui/index.html
```

Cette interface permet d'explorer tous les endpoints disponibles, de tester les requêtes directement depuis le navigateur et de visualiser les modèles de données.

### Projets

```
POST   /api/v1/projects      # Créer un projet
GET    /api/v1/projects      # Lister tous les projets
GET    /api/v1/projects/me   # Projets de l'utilisateur courant
GET    /api/v1/projects/{id} # Détails d'un projet
DELETE /api/v1/projects/{id} # Supprimer un projet
```

### Gestion des Conteneurs

```
POST /api/v1/projects/{id}/start   # Démarrer le conteneur
POST /api/v1/projects/{id}/stop    # Arrêter le conteneur
POST /api/v1/projects/{id}/restart # Redémarrer le conteneur
GET  /api/v1/projects/{id}/status  # État du conteneur
```

## Projet d'Exemple

Pour tester rapidement un déploiement de projet JavaScript :

- Repo : [`pokemon-finder`](https://github.com/axelfrache/pokemon-finder)
- Commande d’import Git :
  ```bash
  https://github.com/axelfrache/pokemon-finder.git

## Sécurité

L'API utilise JWT (JSON Web Tokens) pour l'authentification. Chaque requête doit inclure un header `Authorization` avec un token Bearer :

```
Authorization: Bearer <votre_token_jwt>
```

## Tests

Exécuter les tests :
```bash
mvn test
```