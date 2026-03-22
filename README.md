# Real-Time Analytics (Kafka + Spring + React)

Ce projet est une preuve de concept (PoC) de traitement de données en temps réel utilisant Kafka Streams et Spring Cloud Stream.

### Fonctionnalités
- **Backend** : Génération d'événements simulés, filtrage des sessions courtes et agrégation des vues par page.
- **Kafka Streams** : Utilisation d'un store d'état local pour le comptage.
- **Frontend** : Dashboard React alimenté par Server-Sent Events (SSE).
- **Infrastructure** : Kafka en mode KRaft via Docker.

### Lancement
```bash
docker compose up --build
```
- Dashboard : http://localhost:5173
- Swagger : http://localhost:8081/swagger-ui.html
- Kafka UI : http://localhost:8080

### Remarques
- Les données sont générées automatiquement au démarrage.
- Les compteurs sont réinitialisés en cas de suppression des volumes Docker.
