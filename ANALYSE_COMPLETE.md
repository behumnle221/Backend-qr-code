# 📊 ANALYSE COMPLÈTE - Backend QR Code Payment System

## 🎯 Vue d'ensemble du projet

**Projet :** Système de paiement par QR Code (POS Payment System)
**Framework :** Spring Boot 3.2.1 + Spring Security + JWT
**Base de données :** MySQL 8.0
**Java :** JDK 17
**Build Tool :** Gradle

**Objectif :** Plateforme permettant aux vendeurs de générer des QR codes de paiement, et aux clients de payer via ces QR codes avec intégration du gateway Aangaraa Pay.

---

## 📦 Architecture et Structure

```
Backend-qr-code/backend/src/main/java/com/fapshi/backend/
├── config/                    # Configuration Spring
├── controller/                # API REST Endpoints
├── dto/                       # Data Transfer Objects
│   ├── request/              # DTOs de requête
│   ├── response/             # DTOs de réponse
│   └── external/             # DTOs pour APIs externes
├── entity/                    # Entités JPA (modèles de données)
├── enums/                     # Énumérations
├── exception/                 # Classes d'exception
├── repository/               # Data Access Layer (JPA)
├── security/                 # Sécurité JWT & Authentication
├── service/                  # Logique métier
└── utils/                    # Utilitaires

BackendApplication.java       # Point d'entrée Spring Boot
```

---

## 🏗️ Modèle de Données (Entités JPA)

### 1️⃣ **User** (Classe abstraite - Inheritance SINGLE_TABLE)
```
user_type (discriminator)
├── ADMIN    ➜ Admin
├── CLIENT   ➜ Client
└── VENDEUR  ➜ Vendeur
```
- **Champs communs :** id, nom, email, telephone, password (hashé), dateInscription
- **Stratégie :** Single Table Inheritance (tous dans la table `users`)

### 2️⃣ **Vendeur extends User**
- Champs supplémentaires : `nomCommerce`, `adresse`
- Génère et manage les QR codes

### 3️⃣ **Client extends User**
- Pas de champs supplémentaires actuellement
- Effectue les paiements via les QR codes

### 4️⃣ **QRCode**
- `id` : identifiant unique
- `contenu` : données du QR (texte libellé)
- `montant` : BigDecimal (montant en XAF)
- `description` : libellé du produit/service
- `vendeur_id` : référence au vendeur propriétaire
- `dateCreation`, `dateExpiration` : LocalDateTime
- `estUtilise` : booléen (utilisé 1 fois = ne peut pas être réutilisé)
- `hash` : identifiant unique du QR

### 5️⃣ **Transaction**
- `id` : identifiant unique
- `qr_code_id` : FK vers QRCode (obligatoire)
- `client_id` : FK vers Client (optionnel - authentification pas obligatoire)
- `telephoneClient` : téléphone du client (pour opérateurs comme Orange/MTN)
- `montant` : BigDecimal (montant à payer)
- `statut` : PENDING | SUCCESS | FAILED | EXPIRED | CANCELLED
- `payToken` : token retourné par Aangaraa Pay
- `payUrl` : URL de redirection (mode redirection)
- `referenceOperateur` : ID de transaction chez Aangaraa
- `operator` : "Orange_Cameroon" | "MTN_Cameroon"
- `dateCreation` : LocalDateTime (auto via @PrePersist)
- `dateExpiration` : LocalDateTime

### 6️⃣ **Admin extends User**
- Champ : `role = "ADMIN"` (booléen/String)

### 7️⃣ **ApiCredentials**
- Stocke les clés d'API pour les applications intégrant le système
- `appKey` : clé d'authentification
- `nomApplication` : nom de l'app
- `environnement` : TEST | PROD
- `actif` : booléen
- `derniereUtilisation` : LocalDateTime

### 8️⃣ **ConfigurationFrais**
- Configuration des frais du système
- `tauxPlateforme` : 2% (prélevé sur chaque transaction)
- `fraisRetraitFixe` : 100 XAF (frais fixes pour retrait)
- `montantMinimum` : 100 XAF
- `montantMaximum` : 500 000 XAF

### 9️⃣ **WebhookNotification**
- Stocke les webhooks reçus d'Aangaraa Pay
- `payToken`, `status`, `message`, `transactionIdExterne`
- `traite` : booléen (si webhook a été traité)
- `tentatives` : nombre de tentatives de traitement
- `dateReception` : LocalDateTime

### 🔟 **ResetToken**
- Tokens de réinitialisation de mot de passe
- `token` : code unique (6 chiffres)
- `userId` : FK vers User
- `expiryDate` : LocalDateTime (expiration 10 minutes)

---

## 🎮 Controllers (API Endpoints)

### 📍 **AuthController** (`/api/auth`)
| Endpoint | Méthode | Authentification | Description |
|----------|---------|------------------|-------------|
| `/register/client` | POST | ❌ Non | Inscription client |
| `/register/vendeur` | POST | ❌ Non | Inscription vendeur |
| `/login` | POST | ❌ Non | Connexion générale |
| `/forgot-password` | POST | ❌ Non | Demande réinitialisation |
| `/reset-password` | POST | ❌ Non | Réinitialisation mot de passe |
| `/me` | GET | ✅ JWT | Infos utilisateur connecté |

✨ **Retourne :** JWT token valide 24h (`expirationTime = 86400000L`)

---

### 📍 **QRCodeController** (`/api/qr`)
| Endpoint | Méthode | Auth | Role | Description |
|----------|---------|------|------|-------------|
| `/generate` | POST | ✅ JWT | VENDEUR | Générer un QR code |
| `/my-qrs` | GET | ✅ JWT | VENDEUR | Liste des QR codes du vendeur |
| `/validate/{qrCodeId}` | GET | ✅ JWT | ANY | Valider un QR code |
| `/mark-as-used/{qrCodeId}` | PUT | ✅ JWT | VENDEUR | Marquer QR comme utilisé |

✨ **Sécurité :** @PreAuthorize("hasAuthority('VENDEUR')")

---

### 📍 **PaymentController** (`/api/payments`)
| Endpoint | Méthode | Auth | Role | Description |
|----------|---------|------|------|-------------|
| `/initiate` | POST | ✅ JWT | CLIENT/VENDEUR | Initier un paiement |

⚠️ **Actuellement très minimal** - À enrichir (status, confirmation, webhook handling)

---

### 📍 **ClientController** (`/api/client`)
**Status :** ❌ Vide (à implémenter)

Doit contenir :
- Historique des transactions
- Profil utilisateur
- Paramètres de compte

---

### 📍 **VendeurController** (`/api/vendeur`)
**Status :** ❌ Vide (à implémenter)

Doit contenir :
- Historique des ventes
- Dashboard/statistiques
- Gestion des QR codes
- Profil commerce

---

## 🔐 Sécurité (Security)

### 🔑 **JWT Configuration**
- **Algorithme :** HS256 (HMAC-SHA256)
- **Durée de vie :** 24 heures (86400000 ms)
- **Clé secrète :** Générée dynamiquement par `Keys.secretKeyFor(SignatureAlgorithm.HS256)`
- **Claims :** username, userId, role

**Exemple de token :**
```json
{
  "sub": "jean@email.com",
  "userId": 1,
  "role": "VENDEUR",  // SANS le préfixe ROLE_
  "iat": 1707559123,
  "exp": 1707645523
}
```

### 🛡️ **JwtAuthenticationFilter**
- Intercepte toutes les requêtes (sauf `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`)
- Extrait le token du header `Authorization: Bearer <token>`
- Valide et crée l'authentification Spring Security
- Ajoute l'authority `ROLE_<role>` automatiquement

### 🚪 **SecurityConfig**
- **CSRF :** Désactivé (API REST stateless)
- **CORS :** Désactivé pour les tests
- **Session :** STATELESS (aucune session serveur)
- **Routes protégées :**
  - `/api/qr/generate` → `VENDEUR` seulement
  - `/api/payments/**` → `CLIENT` ou `VENDEUR`
  - `/api/auth/**` → Publique
  - Autres → Authentifiées

---

## 💼 Services (Métier)

### 🎯 **QRCodeService**
```java
- save(QRCode)                                    // Créer/sauver un QR
- findByVendeurId(Long vendeurId)               // QR codes du vendeur
- findNonUtilises()                             // QR codes disponibles
- validateQrCode(Long qrCodeId)                 // Valider un QR (expiration, utilisation)
- markQrAsUsed(Long qrCodeId, Long vendeurId)  // Marquer comme utilisé
```

### 💳 **PaymentService**
```java
- initiatePayment(InitiatePaymentRequest)      // Initier paiement (appel API Aangaraa)
- validateRequest()                             // Vérifications basiques
- prepareAangaraaPayload()                      // Construire payload API
- [À implémenter] confirmPayment()              // Confirmer paiement
- [À implémenter] handleWebhook()               // Traiter webhook Aangaraa
- [À implémenter] markTransactionAsSuccess()    // Finaliser après paiement
```

### 📊 **TransactionService**
```java
- save(Transaction)
- findById(Long)
- findAll()
- findByClientId(Long)                          // Historique client
- findByVendeurId(Long)                         // Via QR code vendor
```

### 👤 **UserService**
```java
- save(User)
- findByEmail(String)
- findByTelephone(String)
- existsByEmail(), existsByTelephone()
- requestPasswordReset(String email)            // Envoyer code reset
- resetPassword(String code, String newPassword)// Réinitialiser mot de passe
```

### 🏪 **VendeurService**
```java
- save(), findById(), findAll()
- findByEmail(), findByTelephone()
```

### 👥 **ClientService**
```java
- save(), findById(), findAll()
- findByEmail(), findByTelephone()
```

### 📧 **EmailService**
```java
- sendResetCodeEmail(to, code, userName)       // Email HTML avec code 6 chiffres
```

### 🔧 Autres Services
- **AdminService**, **ApiCredentialsService**, **ConfigurationFraisService**
- **AangaraaPayRequestService**, **AangaraaPayResponseService**
- **StatusCheckService**, **WebhookNotificationService**

---

## 🗄️ Repositories (Data Access)

Tous implémentent `JpaRepository<T, Long>` :

| Repository | Méthodes importantes |
|------------|---------------------|
| **UserRepository** | findByEmail, findByTelephone, existsByEmail, existsByTelephone |
| **QRCodeRepository** | findByVendeurId, findByEstUtiliseFalse, findByVendeurIdOrderByDateCreationDesc |
| **TransactionRepository** | findByQrCodeVendeurId, findByClientId, findByStatut |
| **ClientRepository** | findByEmail, findByTelephone |
| **VendeurRepository** | findByEmail, findByTelephone |
| **ResetTokenRepository** | findByToken, deleteByUserId |
| **ApiCredentialsRepository** | - |
| **AdminRepository** | - |

---

## 📨 DTOs

### Request DTOs
- `ClientRegisterRequest` : nom, email, telephone, password
- `VendeurRegisterRequest` : nom, email, telephone, password, nomCommerce, adresse
- `LoginRequest` : email/telephone, password
- `GenerateQrRequest` : montant, description, dateExpiration
- `InitiatePaymentRequest` : qrCodeId, telephoneClient, operator, montant, directPayment
- `ForgotPasswordRequest` : email
- `ResetPasswordRequest` : code, newPassword

### Response DTOs
- `ApiResponse<T>` : Enveloppe générique (success, message, data, timestamp)
- `LoginResponse` : token, userId, role, expiresIn
- `UserResponse` : id, nom, email, telephone, role, dateInscription
- `QrCodeResponse` : id, contenu, montant, description, dateExpiration, estUtilise
- `QrCodeSummaryResponse` : id, contenu, montant, description, dateCreation, dateExpiration, estUtilise
- `QrValidationResponse` : valide, message, qrCodeId, montant, description, vendeurNom, vendeurTelephone, dateExpiration, estUtilise
- `PaymentInitResponse` : success, message, transactionId, payToken, payUrl

### External DTOs
- `AangaraaPaymentResponse` : Réponse du gateway Aangaraa Pay

---

## 🔗 Intégration Aangaraa Pay

### Configuration
```yaml
APP_KEY: "NRYT-9742-EHQY-QB4B"
URL_DIRECT: "https://api-production.aangaraa-pay.com/api/v1/no_redirect/payment"
URL_REDIRECT: "https://api-production.aangaraa-pay.com/api/v1/redirect/payment"
```

### Flux Paiement
1. **Client scanne QR** → obtient montant
2. **Client initie paiement** → POST `/api/payments/initiate`
3. **Backend valide QR** (montant, expiration, utilisation)
4. **Backend crée Transaction** (statut = PENDING)
5. **Backend appelle Aangaraa** → reçoit payToken et payUrl
6. **Backend retourne** → payToken et payUrl au client
7. **Client confirme paiement** (mode direct : PIN sur téléphone)
8. **Aangaraa envoie webhook** → Backend traite et met à jour Transaction
9. **QR marqué comme utilisé** (estUtilise = true)

### Modes de Paiement
- **Mode Direct** : Client rentre le PIN sur son téléphone (prompt Aangaraa)
- **Mode Redirection** : Client redirigé vers portail Aangaraa pour paiement

---

## 📧 Email

### Configuration
```yaml
mail:
  host: smtp.gmail.com
  port: 587
  username: pewoparfait@gmail.com
  password: nzzinpcmwaauxdja  # App Password (16 caractères, sans espaces)
  properties.mail.smtp.auth: true
  properties.mail.smtp.starttls.enable: true
```

### Template Email
- Sujet : "Réinitialisation de mot de passe - Qr-CodePay"
- Corps : HTML stylisé avec code 6 chiffres
- Code valide 10 minutes

---

## 🔌 Swagger/OpenAPI

### Configuration
- **URL :** `http://localhost:8080/swagger-ui.html`
- **Docs :** `http://localhost:8080/v3/api-docs`
- **Security Scheme :** Bearer JWT (HTTP)

### Annotation clés
```java
@SecurityRequirement(name = "bearerAuth")      // Sur les controllers protégés
@Operation(summary = "...", description = "...") // Documenter l'endpoint
@ApiResponse(responseCode = "201", description = "...") // Réponses possibles
```

---

## 🛠️ Utilities

### **PasswordEncoderUtil**
- `encode(password)` : Hash avec BCrypt
- `matches(raw, encoded)` : Comparaison

**Provider :** `BCryptPasswordEncoder` (bean dans `SecurityConfig`)

---

## 🧪 Configuration & Build

### Build Tool
- **Gradle** (wrapper: `./gradlew` ou `gradlew.bat`)
- **Plugins :** Spring Boot 3.2.1, Dependency Management 1.1.7

### Database
```yaml
datasource:
  url: jdbc:mysql://localhost:3306/fapshi_db
  username: root
  password: ""
  driver-class-name: com.mysql.cj.jdbc.Driver
  
jpa:
  hibernate.ddl-auto: update  # Auto-création/mise à jour des tables
  show-sql: true
```

### Logging
```yaml
logging:
  level:
    org.springframework.security: DEBUG
    org.springframework.web: DEBUG
    com.fapshi.backend.security: DEBUG
```

### Port
```yaml
server:
  port: 8080
```

---

## 📋 Statuts des Composants

| Composant | Statut | Remarques |
|-----------|--------|----------|
| ✅ Entités | Complet | 10 entités bien structurées |
| ✅ Security (JWT) | Complet | Filter, Config, JwtUtil fonctionnels |
| ✅ AuthController | Complet | Register, Login, Reset Password |
| ✅ QRCodeController | Complet | Generate, Validate, List, Mark |
| ⚠️ PaymentController | 50% | Initiate OK, confirmation/webhook manquants |
| ❌ ClientController | Vide | À implémenter |
| ❌ VendeurController | Vide | À implémenter |
| ✅ Services | 80% | Logique métier implémentée, webhooks à ajouter |
| ✅ Repositories | Complet | Toutes les queries nécessaires |
| ✅ Email | Complet | Service de réinitialisation mot de passe |
| ✅ Configuration | Complet | Spring, Swagger, DB, Email |

---

## 🎯 Points Clés à Retenir

1. **Authentification :** JWT 24h, role-based access control
2. **QR Codes :** Une seule utilisation par QR, dates d'expiration
3. **Transactions :** Liées aux QR codes et optionnellement aux clients
4. **Paiement :** Intégration Aangaraa (2 modes : direct et redirection)
5. **Workflow :** Vendeur génère QR → Client paie via QR → Transaction créée → Webhook confirme
6. **Sécurité :** Single Table Inheritance pour users, BCrypt passwords, JWT tokens

---

## 🚀 Prochaines Étapes Possibles

### 🔴 Priorité Haute
1. Implémenter **webhook handling** pour Aangaraa Pay
2. Compléter **PaymentController** (confirmation, status check)
3. Implémenter **ClientController** (historique, profil)
4. Implémenter **VendeurController** (dashboard, statistiques)

### 🟡 Priorité Moyenne
1. Ajouter **exception handling** global (@ControllerAdvice)
2. Implémenter **validation** robuste (Jakarta Bean Validation)
3. Ajouter **logging** structuré (SLF4J)
4. Implémenter **rate limiting** et **API key auth**

### 🟢 Priorité Basse
1. Ajouter des **tests unitaires** (JUnit 5, Mockito)
2. Implémenter **caching** (Redis)
3. Ajouter **audit trail** pour les transactions
4. Documenter les **API avec exemples** dans Swagger

---

## 📚 Ressources Utiles

- [Spring Security Docs](https://spring.io/projects/spring-security)
- [JWT with Spring](https://auth0.com/blog/spring-boot-java-tutorial-build-secure-app/)
- [Swagger/OpenAPI](https://springdoc.org/)
- [MySQL Connector](https://dev.mysql.com/downloads/connector/j/)
- [Jakarta Bean Validation](https://jakarta.ee/specifications/bean-validation/)

---

**Statut :** ✅ Analyse complète terminée. Vous êtes maintenant prêt pour continuer le développement !

