# 🚀 Projet Gestion Utilisateurs (Angular + Spring Boot + Keycloak)

## 📌 Description
Ce projet est une application de gestion des utilisateurs avec intégration **Keycloak** pour la sécurité.
- **Frontend** : Angular (http://localhost:4200)
- **Backend** : Spring Boot (http://localhost:8086)
- **Sécurité** : OAuth2 + JWT (Keycloak)

L’API permet de :
- Gérer les **utilisateurs**
- Gérer les **groupes/régions**
- Assigner des utilisateurs à des régions

---

## 📂 Architecture
frontend/ → Angular 17 (UI + appels API)
backend/ → Spring Boot 3.5.3 (REST API sécurisée)
keycloak/ → Serveur Keycloak pour authentification

yaml
Copier
Modifier

---

## 🔑 Authentification
Toutes les requêtes vers `/api/**` nécessitent un **token JWT** Keycloak.

### Récupération du token avec Postman
1. Se connecter à Keycloak :
    - URL : `http://localhost:8080/realms/<NOM_REALM>/protocol/openid-connect/token`
    - Méthode : `POST`
    - Body (x-www-form-urlencoded) :
      ```
      client_id=<CLIENT_ID>
      username=<USERNAME>
      password=<PASSWORD>
      grant_type=password
      ```
2. Copier le champ `access_token` de la réponse.
3. Ajouter dans Postman, onglet **Authorization → Bearer Token**.

---

## 📖 Endpoints API

### 👤 Utilisateurs
| Méthode | Endpoint                  | Description                   | Auth |
|---------|---------------------------|-------------------------------|------|
| GET     | `/api/utilisateurs`       | Récupère tous les utilisateurs | ✅   |
| GET     | `/api/utilisateurs/{id}`  | Récupère un utilisateur par ID | ✅   |
| POST    | `/api/utilisateurs`       | Crée un utilisateur            | ✅   |
| DELETE  | `/api/utilisateurs/{id}`  | Supprime un utilisateur        | ✅   |

---

### 🏢 Groupes / Régions
| Méthode | Endpoint             | Description                                         | Auth |
|---------|----------------------|-----------------------------------------------------|------|
| GET     | `/api/groupes/dropdown`       | Récupère tous les groupes avec leurs utilisateurs   | ✅   |
| GET     | `/api/groupes/dropdown/{id}`  | Récupère un groupe spécifique par keycloakId        | ✅   |
| POST    | `/api/groupes`       | Crée un nouveau groupe (body: { code, label })      | ✅   |
| PUT     | `/api/groupes/{keycloakId}`       | Modifie un groupe existant (body: { code, label })  | ✅   |

---

### 🔗 Affectation
| Méthode | Endpoint                                                            | Description                              | Auth |
|---------|---------------------------------------------------------------------|------------------------------------------|------|
| PUT     | `/api/utilisateurs/affecter-region?utilisateurId={id}&regionCode=X` | Assigne un utilisateur à une région       | ✅   |

---

## 🛠️ Comment tester

### 📌 1. Avec Postman
- Importer la collection Postman (si fournie).
- Ajouter le token JWT dans **Authorization → Bearer Token**.
- Lancer les requêtes → faire des **captures d’écran** pour le rapport.

### 📌 2. Avec Angular
- Lancer Angular :
  ```bash
  cd frontend
  ng serve