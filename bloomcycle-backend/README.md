# BloomCycle

BloomCycle est une plateforme conçue pour gérer, déployer, et maintenir en condition opérationnelle des applications. Ce projet est construit avec **Spring Boot** et se concentre sur le déploiement continu et la partie "Run" de l'infrastructure.

## Fonctionnalités principales
1. **Gestion des applications** :
    - Visualiser les applications déployées, leur état (en cours d'exécution, arrêté, crashé, etc.).
    - Gestion via une API REST.
2. **Déploiement** :
    - Support des images Docker pour démarrer et arrêter des applications.
    - Possibilité de lancer plusieurs instances simultanément.
3. **Mise à jour continue** :
    - Support des "rolling updates" sans interruption des utilisateurs connectés.
    - Redémarrage automatique en cas de crash.
4. **Routing HTTP/HTTPS** :
    - Gestion du routage pour les clients, y compris la redirection.
5. **Configuration dynamique** :
    - Gestion des configurations via des variables d'environnement ou un fichier de propriétés.
6. **Monitoring** :
    - Surveillance des applications déployées (logs, métriques de CPU, mémoire, etc.).
7. **Infrastructure mono-machine** :
    - Déploiement ciblé pour une VM unique.

## Prérequis
1. **Environnement** :
    - Java 23.
    - Maven pour la gestion du build.
    - Docker pour la gestion des conteneurs.
2. **Outils** :
    - GitHub/GitLab pour la gestion des versions.
    - IDE (IntelliJ, Eclipse, VS Code) pour le développement.
3. **Accès réseau** :
    - Ports 80 et 443 pour HTTP/HTTPS.
    - Accès à Docker Daemon pour les conteneurs.

## Installation
1. **Cloner le dépôt** :
   ```bash
   git clone https://github.com/votre-repo/bloomdeploy.git
   cd bloomdeploy
