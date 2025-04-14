# BloomCycle

BloomCycle est une plateforme complète conçue pour gérer, déployer et maintenir en condition opérationnelle des applications. Cette solution intègre un backend Spring Boot et un frontend Angular pour offrir une expérience utilisateur fluide dans la gestion des déploiements conteneurisés.

## Fonctionnalités principales

- **Surveillance des applications** : Visualisation des applications en cours d'exécution, arrêtées ou en erreur via API et interface web
- **Support multi-instances** : Capacité à exécuter plusieurs instances d'une même application simultanément
- **Build from source** : Construction automatique à partir des sources (Java, Node.js, Python)
- **Support de sites statiques** : Déploiement simplifié pour les sites web statiques
- **Déploiement conteneurisé** : Support des images Docker avec démarrage à la demande
- **Démarrage et redémarrage** : Contrôle total sur le cycle de vie des applications
- **Redémarrage automatique** : Détection et relance automatique en cas de crash
- **Rolling updates** : Mise à jour sans interruption de service pour les utilisateurs connectés
- **Mono-machine** : Optimisé pour fonctionner sur une seule VM
- **Routing HTTP/HTTPS** : Support intégré avec redirection automatique
- **Custom domains** : Possibilité d'associer des noms de domaine personnalisés
- **Logs & métriques** : Suivi en temps réel de l'utilisation CPU, mémoire et autres ressources
- **Multi-tenant** : Isolation des données et visibilité limitée aux ressources de l'utilisateur
- **Authentification JWT** : Sécurisation des accès avec JSON Web Tokens
- **Gestion des accès utilisateurs** : Chaque utilisateur n'a accès qu'à ses propres projets
- **API REST documentée** : Documentation complète via OpenAPI/Swagger
- **Interface utilisateur intuitive** : Développée avec Angular et DaisyUI

## Prérequis

- Docker et Docker Compose
- Git
- Java 23+
- Node.js et npm

## Installation et démarrage

### Option 1: Déploiement avec Docker Compose (recommandé)

1. **Cloner le dépôt**
   ```bash
   git clone https://github.com/axelfrache/bloomcycle.git
   cd bloomcycle
   ```

2. **Lancer l'application avec Docker Compose**
   ```bash
   docker-compose up -d
   ```

   Cette commande va:
   - Construire les images Docker pour le frontend et le backend
   - Créer et configurer la base de données MySQL
   - Démarrer tous les services nécessaires

3. **Accéder à l'application**
   - Frontend: http://localhost:4200
   - API Backend: http://localhost:9090
   - Documentation API: http://localhost:9090/swagger-ui/index.html

### Option 2: Installation pour le développement

#### Backend (Spring Boot)

1. **Naviguer vers le répertoire backend**
   ```bash
   cd bloomcycle-backend
   ```

2. **Compiler le projet avec Maven**
   ```bash
   ./mvnw clean install
   ```

3. **Lancer l'application**
   À noter : Pour exécuter l'application sans Docker, vous devez soit :
   - Disposer d'une base de données PostgreSQL configurée selon les paramètres dans `application.properties`
   - Lancer l'application avec le profil de développement qui utilise une base de données H2 en mémoire :
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

   Le backend sera accessible sur http://localhost:9090

#### Frontend (Angular)

1. **Naviguer vers le répertoire frontend**
   ```bash
   cd bloomcycle-frontend
   ```

2. **Installer les dépendances**
   ```bash
   npm install
   ```

3. **Lancer le serveur de développement**
   ```bash
   ng serve
   ```

   Le frontend sera accessible sur http://localhost:4200

## Architecture du projet

```
bloomcycle/
├── bloomcycle-backend/         # Application Spring Boot
│   ├── src/                    # Code source Java
│   ├── Dockerfile              # Configuration Docker pour le backend
│   └── pom.xml                 # Configuration Maven
├── bloomcycle-frontend/        # Application Angular
│   ├── src/                    # Code source TypeScript/Angular
│   ├── Dockerfile              # Configuration Docker pour le frontend
│   └── package.json            # Configuration npm
├── docker-compose.yml          # Configuration Docker Compose
└── docker-compose.override.yml # Surcharge de configuration pour Docker Compose
```

## Accès aux projets déployés

BloomCycle utilise Traefik comme reverse proxy pour le routage HTTP dynamique. Les projets déployés sont accessibles via des sous-domaines au format:

```
project-{id}.bloomcycle.localhost
```

Le dashboard Traefik est accessible sur:
```
http://localhost:8080
```

## Documentation de l'API

### Swagger UI

La documentation complète de l'API REST est disponible via Swagger UI à l'adresse:
```
http://localhost:9090/swagger-ui/index.html
```

Cette interface permet de:
- Explorer tous les endpoints disponibles
- Tester les requêtes directement depuis le navigateur
- Visualiser les modèles de données
- Comprendre les paramètres requis pour chaque endpoint

### Principaux endpoints

#### Gestion des projets
```
POST   /api/v1/projects      # Créer un projet
GET    /api/v1/projects      # Lister tous les projets
GET    /api/v1/projects/me   # Projets de l'utilisateur courant
GET    /api/v1/projects/{id} # Détails d'un projet
DELETE /api/v1/projects/{id} # Supprimer un projet
```

#### Gestion des conteneurs
```
POST /api/v1/projects/{id}/start   # Démarrer le conteneur
POST /api/v1/projects/{id}/stop    # Arrêter le conteneur
POST /api/v1/projects/{id}/restart # Redémarrer le conteneur
GET  /api/v1/projects/{id}/status  # État du conteneur
```

### Authentification

L'API utilise JWT (JSON Web Tokens) pour l'authentification. Chaque requête doit inclure un header `Authorization` avec un token Bearer:

```
Authorization: Bearer <votre_token_jwt>
```

Pour obtenir un token, utilisez les endpoints d'authentification:
```
POST /api/v1/auth/login    # Connexion
POST /api/v1/auth/register # Inscription
```

## Configuration

### Variables d'environnement

Le fichier `docker-compose.override.yml` permet de personnaliser la configuration:

```yaml
services:
  backend:
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/bloomcycle
      SPRING_DATASOURCE_USERNAME: bloomuser
      SPRING_DATASOURCE_PASSWORD: bloompassword
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
      SPRING_JPA_DATABASE_PLATFORM: org.hibernate.dialect.MySQLDialect

  mysql:
    environment:
      MYSQL_DATABASE: bloomcycle
      MYSQL_USER: bloomuser
      MYSQL_PASSWORD: bloompassword
      MYSQL_ROOT_PASSWORD: rootpassword
```

## Tests

### Backend
```bash
cd bloomcycle-backend
./mvnw test
```

### Frontend
```bash
cd bloomcycle-frontend
npm run test
```

## Contribution

1. Fork le projet
2. Créez votre branche de fonctionnalité (`git checkout -b feature/amazing-feature`)
3. Committez vos changements (`git commit -m 'Add some amazing feature'`)
4. Push vers la branche (`git push origin feature/amazing-feature`)
5. Ouvrez une Pull Request
