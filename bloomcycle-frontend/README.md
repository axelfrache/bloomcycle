# Documentation Frontend BloomCycle

## Vue d'ensemble

Le frontend de BloomCycle est développé avec Angular et fournit une interface utilisateur pour gérer des projets conteneurisés. Il permet aux utilisateurs de visualiser les projets, surveiller leur état, et effectuer des opérations comme démarrer, arrêter, et redémarrer des conteneurs.

## Architecture technique

- **Framework** : Angular
- **Bibliothèque UI** : DaisyUI avec TailwindCSS

## Structure du projet

```
bloomcycle-frontend/
├── src/
│   ├── app/
│   │   ├── core/                   # Services, modèles, intercepteurs
│   │   │   ├── guards/             # Gardiens de route
│   │   │   ├── interceptors/       # Intercepteurs HTTP
│   │   │   ├── models/             # Interfaces et classes de modèles
│   │   │   └── services/           # Services partagés
│   │   ├── features/               # Modules fonctionnels
│   │   │   ├── details/            # Page de détails du projet
│   │   │   ├── home/               # Page d'accueil
│   │   │   ├── login/              # Page de connexion
│   │   │   ├── register/           # Page d'inscription
│   │   │   └── upload/             # Page d'upload
│   │   ├── app.component.ts        # Composant racine
│   │   ├── app.component.html      # Point d'entrée HTML de l'application
│   │   ├── app.component.spec.ts   # Tests unitaires du composant racine
│   │   ├── app.config.ts           # Configuration de l'application
│   │   ├── app.module.ts           # Module principal de l'application
│   │   └── app.routes.ts           # Configuration des routes
│   └── assets/                 # Ressources statiques
└── angular.json                # Configuration Angular
```

## Composants

### Composant Home
Emplacement : `src/app/features/home/home.component.ts`
- Affiche la liste de tous les projets
- Point d'entrée pour les utilisateurs après authentification
- Implémenté comme un composant autonome
- Fonctionnalités :
  - Affichage de la liste des projets avec leur statut
  - Navigation vers les détails d'un projet

### Composant Details
Emplacement : `src/app/features/details/details.component.ts`
- Affiche les informations détaillées d'un projet spécifique
- Montre l'état du projet, l'utilisation des ressources et les logs
- Fournit des contrôles pour démarrer, arrêter et redémarrer les conteneurs
- Informations disponibles :
  - ID du projet
  - Nom du projet
  - Statut du conteneur
  - CPU et mémoire utilisés
  - URL d'accès au projet
  - Logs

### Composant Login
Emplacement : `src/app/features/login/login.component.ts`
- Gère l'authentification des utilisateurs
- Permet aux utilisateurs de se connecter avec un e-mail et un mot de passe
- Point d'entrée pour les utilisateurs non authentifiés
- Fonctionnalités :
  - Formulaire de connexion
  - Redirection vers la page d'accueil après connexion réussie

### Composant Register
Emplacement : `src/app/features/register/register.component.ts`
- Permet aux nouveaux utilisateurs de créer un compte
- Fonctionnalités :
  - Formulaire d'inscription
  - Redirection vers la page de connexion après inscription réussie

### Composant Upload
Emplacement : `src/app/features/upload/upload.component.ts`
- Permet aux utilisateurs de télécharger des fichiers pour déployer un projet
- Fonctionnalités :
  - Drag and drop pour le téléchargement de fichiers
  - Téléchargement à partir de GitHub

## Modèles de données

### Modèle Project
Emplacement : `src/app/core/models/project.model.ts`
```typescript
export interface Project {
  id: string;
  name: string;
  containerStatus: string;  // 'running', 'stopped', 'crashed'
  owner: User;
  cpuUsage: number;        // Pourcentage d'utilisation CPU
  memoryUsage: number;     // Utilisation mémoire en MB
  serverUrl: string;       // URL d'accès au projet déployé
  technology: string;      // Technologie utilisée (Node.js, Java, etc.)
}
```

### Modèle User
Emplacement : `src/app/core/models/user.model.ts`
```typescript
export interface User {
  id: string;
  username: string;
  email: string;
  role: string;          // 'admin', 'developer', etc.
}
```

## Services

### ProjectService
Emplacement : `src/app/core/services/project.service.ts`
- Gère la communication API pour les opérations sur les projets
- Méthodes principales :
  - `getProjects()`: Récupère tous les projets de l'utilisateur authentifié
  - `getProjectById(id: string)`: Récupère un projet par son ID
  - `getProjectDetails(id: string)`: Récupère les informations d'un projet
  - `startProject(id: string)`: Démarre un conteneur
  - `stopProject(id: string)`: Arrête un conteneur
  - `restartProject(id: string)`: Redémarre un conteneur

### AuthService
Emplacement : `src/app/core/services/auth.service.ts`
- Gère l'authentification des utilisateurs
- Stocke le token JWT dans le localStorage
- Vérifie les permissions utilisateur pour certaines actions

## Structure des modules

L'application utilise des modules Angular pour organiser les fonctionnalités :

### HomeModule
```typescript
@NgModule({
  imports: [CommonModule, HomeComponent],
  exports: [HomeComponent]
})
export class HomeModule {}
```

### DetailsModule
```typescript
@NgModule({
  imports: [CommonModule, RouterModule, DetailsComponent],
  exports: [DetailsComponent]
})
export class DetailsModule {}
```

## Design UI

- Utilisation des composants DaisyUI basés sur TailwindCSS
- Design responsive pour différentes tailles d'écran
- Indicateurs d'état avec code couleur :
  - En cours d'exécution : Vert
  - Arrêté : Jaune
  - Planté : Rouge
- Boutons d'action contextuels :
  - Bouton Démarrer (vert)
  - Bouton Arrêter (rouge)
  - Bouton Redémarrer (vert)

## Fonctionnalités clés

1. Surveillance des projets avec statut en temps réel
2. Suivi de l'utilisation des ressources (CPU, Mémoire)
3. Visualisation des logs des conteneurs
4. Opérations de gestion des conteneurs
5. Design responsive pour ordinateur et mobile
6. Authentification et gestion des droits utilisateurs

## Utilisation des routes

- `/home` - Page d'accueil avec liste des projets
- `/login` - Page de connexion
- `/register` - Page d'inscription
- `/projects/:id` - Page de détails d'un projet
- `/upload` - Page de téléchargement de fichiers
