# Real-Time Analytics Dashboard (Spring Cloud Stream + Kafka + React)

Ce projet est une application full-stack démontrant le traitement de flux de données en temps réel. Il utilise **Spring Cloud Stream Kafka Streams** pour le traitement des événements côté backend et **React** avec des graphiques dynamiques pour la visualisation en direct.

## 🚀 Architecture Technique

L'application repose sur une architecture orientée événements (EDA) :

* **Message Broker** : Apache Kafka & Zookeeper pour la gestion des flux.
* **Backend** : Spring Boot 3 utilisant Kafka Streams pour agréger le trafic par page sur des fenêtres glissantes de 5 secondes.
* **Frontend** : Application React (Vite) utilisant des **Server-Sent Events (SSE)** pour recevoir les mises à jour en continu sans rechargement.
* **Conteneurisation** : Orchestration complète via Docker Compose.

## 🛠 Stack Technique

| Composant | Technologie |
| --- | --- |
| **Backend** | Java 17, Spring Boot, Spring Cloud Stream, Kafka Streams, WebFlux |
| **Frontend** | React 19, Vite, SmoothieCharts (streaming de données) |
| **Infrastructure** | Kafka, Zookeeper, Nginx, Docker |

## ⚙️ Fonctionnalités

* **Producteur Automatique** : Un `Supplier` génère des événements de trafic aléatoires (`P1` ou `P2`) chaque seconde.
* **Traitement Stream** : Filtrage des événements (durée > 100ms) et comptage en temps réel via un store d'état local (`count-store`).
* **Analytics API** : Un endpoint SSE (`/analytics`) diffuse les données agrégées au format JSON.
* **Dashboard Dynamique** : Visualisation de la vélocité du trafic avec des courbes de Bézier fluides et un indicateur de statut de connexion.

## 🚦 Démarrage Rapide

### Prérequis

* Docker et Docker Compose installés.

### Installation et Lancement

1. Clonez le dépôt.
2. Lancez l'infrastructure complète depuis la racine du projet :
```bash
docker compose up --build

```


3. Accédez aux services :
* **Dashboard (Frontend)** : [http://localhost:5173](https://www.google.com/search?q=http://localhost:5173)
* **Backend API** : [http://localhost:8081](https://www.google.com/search?q=http://localhost:8081)



## 🔌 Points d'entrée (Endpoints)

### Backend (`spring-backend`)

* `GET /publish?name=P1&topic=page-events` : Publie manuellement un événement de page.
* `GET /analytics` : Flux SSE retournant les statistiques de trafic en temps réel.

## 🐳 Configuration Docker

Le fichier `docker-compose.yaml` définit quatre services principaux :

* **zookeeper / broker** : Infrastructure Kafka configurée avec des listeners internes et externes.
* **backend** : Application Spring Boot avec vérification de l'état de santé (healthcheck) du broker Kafka.
* **frontend** : Serveur Nginx servant l'application React et configuré comme reverse proxy pour rediriger `/analytics` et `/publish` vers le backend.

## 📝 Logique de Traitement

La transformation des données est gérée par le bean `kStreamKStreamFunction` :

1. **Filtrage** : Exclusion des sessions trop courtes.
2. **Fenêtrage** : Agrégation par `TimeWindows` de 5 secondes.
3. **Matérialisation** : Stockage des résultats dans un store persistant pour interrogation interactive.

---
