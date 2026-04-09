# Projet Soutenance - Résumé Technique

## Vue d'ensemble
Projet de paiement QR Code avec système de recharge et retraits via API Aangaraa-Pay.

---

## 1. Structure des Données

### Entités principales

| Entité | Description |
|-------|-------------|
| `User` | Classe de base (email, nom, telephone) |
| `Client` | Hérite de User + `soldeVirtuel` |
| `Vendeur` | Hérite de User + `soldeVirtuel` + `nomCommerce` |
| `QRCode` | QR Code généré par le vendeur |
| `Transaction` | Paiement/Rechargement (type: PAYMENT_MARCHAND, RECHARGEMENT, RETRAIT) |
| `Retrait` | Demande de retrait |

### Enum TypeTransaction
```java
PAYEMENT_MARCHAND  // Paiement QR normal
RECHARGEMENT      // Rechargement crédit virtuel
RETRAIT           // Retrait vers mobile money
TRANSFERT_VIRTUEL  // Transfert entre clients
```

---

## 2. Endpoints API

### Client Controller (`/api/client`)

| Méthode | Endpoint | Description |
|--------|----------|-------------|
| GET | `/transactions` | Liste des transactions |
| GET | `/solde` | Solde virtuel |
| POST | `/recharger` | Recharger le compte |
| POST | `/retraits` | Demander un retrait |
| GET | `/retraits` | Liste des retraits |
| POST | `/retraits/sync` | Synchroniser retraits PENDING |

### Vendeur Controller (`/api/vendeur`)

| Méthode | Endpoint | Description |
|--------|----------|-------------|
| POST | `/generate-qr` | Générer QR Code |
| POST | `/retraits` | Demander un retrait |
| GET | `/retraits` | Liste des retraits |
| POST | `/retraits/sync` | Sync retraits PENDING |
| POST | `/retraits/sync-legacy` | Sync anciens retraits |

### Webhook (`/api/webhook`)

| Méthode | Endpoint | Description |
|--------|----------|-------------|
| POST | `/aangaraa` | Réception confirmation Aangaraa |

---

## 3. Flux de Paiement

### Rechargement Client
```
1. Client POST /api/client/recharger (montant, opérateur, telephone)
2. PaymentService.initierRechargement()
3. Transaction créée (statut: PENDING)
4. Appel API Aangaraa -> génère payToken
5. Client reçoit SMS/USSD pour confirmer
6. Aangaraa webhook vers /api/webhook/aangaraa
7. WebhookController traite -> crédite soldeVirtuel
```

### Retrait Client/Vendeur
```
1. POST /api/client/retraits (montant, opérateur, téléphone)
2. Vérification solde suffisant
3. Vérifié écart 5min depuis dernier retrait
4. Appel API Aangaraa effectuerRetraitVersMobile()
5. Si statusCode=201 -> SUCCESS immédiat + débit
6. Sinon PENDING -> scheduler vérifie après 5min
7. Scheduler credit/debit automatiquement
```

---

## 4. Corrections.Importantes

### Problème 1: referenceId non utilisé pour sync
- **Symptôme**: Retraits restaient PENDING
- **Cause**: Le code cherchait `status: "SUCCESS"` qui était null
- **Solution**: Vérifier `statusCode == 201`

### Problème 2: Solde non crédité après rechargement
- **Symptôme**: Solde = 0 après validation
- **Cause**: Webhook ne créditait pas le client pour RECHARGEMENT
- **Solution**: Ajouter logique spécifique dans handleSuccess()

### Problème 3: Retraits vendor aussi lents
- **Solution**: Même fix statusCode=201 ajouté

---

## 5. Modifications Base de Données

### Colonnes ajoutées manuellement

```sql
-- Rendre qr_code_id nullable pour rechargements
ALTER TABLE transactions MODIFY COLUMN qr_code_id BIGINT NULL;

-- Rendre vendeur_id nullable pour retraits client
ALTER TABLE retraits MODIFY COLUMN vendeur_id BIGINT NULL;
```

### Hibernate ddl-auto=update
Les autres colonnes sont créées automatiquement (ex: client_id dans retraits).

---

## 6. Sauvegarde des Réponses Aangaraa

### Tables
- `aangaraa_pay_requests` - Requêtes envoyées
- `aangaraa_pay_responses` - Réponses reçues

### Services qui sauvegardent
- `PaymentService` - Rechargements
- `AangaraaWithdrawalService` - Retraits

---

## 7. Scheduler (Tâches automatisées)

### checkPendingTransactions() - Toutes les 30s
Vérifie les transactions PENDING via API Aangaraa

### checkPendingRetraits() - Toutes les 30s
Vérifie les retraits PENDING via API Aangaraa

---

## 8. Variables d'environnement (application.properties)

```properties
# BDD
spring.datasource.url=jdbc:mysql://...

# JWT
app.jwt.secret=...
app.jwt.expiration=...

# Aangaraa
app.aangaraa.api-key=NRYT-...
app.aangaraa.api-url=https://api-production.aangaraa-pay.com
app.aangaraa.webhook-url=

# Frontend
app.frontend.url=http://localhost:5173
```

---

## 9. Stack Technique

| Technologie | Version |
|-------------|---------|
| Java | 17 |
| Spring Boot | 3.2.1 |
| MySQL (Aiven) | 8.x |
| Hibernate | 6.4 |
| Maven/Gradle | - |

---

## 10. Points à improve (futur)

- Tests unitaires/d'intégration
- Pagination complète
- Notifications push (Pusher)
- Gestion erreurs détaillée
- Logs dans fichiers

---

## 11. Commandes Utiles

```bash
# Compiler
./gradlew compileJava

# Build
./gradlew bootJar

# Tests
./gradlew test
```

---

## 12. Fichiers Clés

| Fichier | Rôle |
|--------|------|
| `PaymentService.java` | Logique rechargement/paiement |
| `AangaraaWithdrawalService.java` | Logique retrait |
| `WebhookController.java` | Traitement webhooks |
| `ClientController.java` | Endpoints client |
| `VendeurController.java` | Endpoints vendeur |
| `Transaction.java` | Entity transaction |
| `Retrait.java` | Entity retrait |

---

*Document généré le 2026-04-07*