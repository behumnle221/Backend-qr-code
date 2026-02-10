# 🧪 TESTS ÉTAPE 1 - EXEMPLES DE REQUÊTES

## 📌 BASE URL
```
http://localhost:8080
```

---

## 1️⃣ INSCRIPTION VENDEUR

### Créer un vendeur pour les tests

```bash
curl -X POST http://localhost:8080/api/auth/register/vendeur \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Jean Dupont",
    "email": "jean@test.com",
    "telephone": "237690000001",
    "password": "password123",
    "nomCommerce": "Boutique Jean",
    "adresse": "Yaoundé, Cameroun"
  }'
```

**Réponse Attendue (201)** :
```json
{
  "success": true,
  "message": "Client inscrit avec succès",
  "data": {
    "id": 1,
    "nom": "Jean Dupont",
    "email": "jean@test.com",
    "telephone": "237690000001",
    "role": "VENDEUR",
    "dateInscription": "2026-02-10 10:30:00"
  },
  "timestamp": "2026-02-10 10:30:00"
}
```

---

## 2️⃣ CONNEXION

### Récupérer le JWT token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "jean@test.com",
    "password": "password123"
  }'
```

**Réponse Attendue (200)** :
```json
{
  "success": true,
  "message": "Connexion réussie",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1,
    "role": "VENDEUR",
    "expiresIn": 86400
  },
  "timestamp": "2026-02-10 10:31:00"
}
```

⚠️ **Sauvegardez le token !** Vous l'utiliserez dans les requêtes suivantes.

---

## 3️⃣ VÉRIFIER SOLDE INITIAL

### Consulter le solde du vendeur

```bash
TOKEN="<copier le token ici>"

curl -X GET http://localhost:8080/api/vendeur/solde \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

**Réponse Attendue (200)** :
```json
{
  "success": true,
  "message": "Solde récupéré avec succès",
  "data": {
    "solde": 0.00,
    "devise": "XAF",
    "derniereMiseAJour": null,
    "message": "Solde mis à jour avec succès"
  },
  "timestamp": "2026-02-10 10:32:00"
}
```

✅ Le solde est à 0 car aucune transaction SUCCESS n'a eu lieu.

---

## 4️⃣ CRÉER UN CLIENT (pour le paiement)

```bash
curl -X POST http://localhost:8080/api/auth/register/client \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Paul Client",
    "email": "paul@test.com",
    "telephone": "237690000002",
    "password": "password123"
  }'
```

---

## 5️⃣ CRÉER UN QR CODE

### Le vendeur crée un QR code

```bash
TOKEN="<token du vendeur>"

curl -X POST http://localhost:8080/api/qr/generate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "montant": 15000.00,
    "description": "Achat de produits électroniques",
    "dateExpiration": "2026-02-20T23:59:59"
  }'
```

**Réponse Attendue (201)** :
```json
{
  "success": true,
  "message": "QR Code généré avec succès",
  "data": {
    "id": 1,
    "contenu": "QR pour paiement 15000 XAF",
    "montant": 15000.00,
    "description": "Achat de produits électroniques",
    "dateExpiration": "2026-02-20T23:59:59",
    "estUtilise": false
  },
  "timestamp": "2026-02-10 10:33:00"
}
```

⚠️ **Sauvegardez l'ID du QR (1)** - vous l'utiliserez pour le paiement.

---

## 6️⃣ INITIER UN PAIEMENT

### Client paie via QR code

```bash
TOKEN_CLIENT="<token du client>"

curl -X POST http://localhost:8080/api/payments/initiate \
  -H "Authorization: Bearer $TOKEN_CLIENT" \
  -H "Content-Type: application/json" \
  -d '{
    "qrCodeId": 1,
    "telephoneClient": "237690000002",
    "operator": "Orange_Cameroon",
    "montant": 15000.00,
    "directPayment": true
  }'
```

**Réponse Attendue (200)** :
```json
{
  "success": true,
  "message": "Paiement initié. Veuillez valider sur votre téléphone.",
  "data": {
    "success": true,
    "message": "Paiement initié. Veuillez valider sur votre téléphone.",
    "transactionId": 1,
    "payToken": "pay_abc123def456",
    "payUrl": null
  },
  "timestamp": "2026-02-10 10:34:00"
}
```

⚠️ **Sauvegardez la transactionId (1)** - vous l'utiliserez pour tester le webhook.

---

## 7️⃣ TESTER LE WEBHOOK (SUCCÈS)

### Simuler une notification Aangaraa (SUCCESS)

```bash
# Marquer la transaction comme SUCCESS via webhook
curl -X POST http://localhost:8080/api/webhook/test-aangaraa \
  -H "Content-Type: application/json" \
  -d 'transactionId=1&status=SUCCESS'
```

**Réponse Attendue (200)** :
```json
{
  "success": true,
  "message": "Test webhook exécuté",
  "data": {
    "success": "true",
    "transactionId": "1",
    "statut": "SUCCESS"
  },
  "timestamp": "2026-02-10 10:35:00"
}
```

---

## 8️⃣ VÉRIFIER SOLDE MIS À JOUR

### Consulter le solde APRÈS paiement SUCCESS

```bash
TOKEN="<token du vendeur>"

curl -X GET http://localhost:8080/api/vendeur/solde \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

**Réponse Attendue (200)** :
```json
{
  "success": true,
  "message": "Solde récupéré avec succès",
  "data": {
    "solde": 15000.00,
    "devise": "XAF",
    "derniereMiseAJour": "2026-02-10 10:35:00",
    "message": "Solde mis à jour avec succès"
  },
  "timestamp": "2026-02-10 10:36:00"
}
```

✅ **LE SOLDE A ÉTÉ AUGMENTÉ À 15000 XAF !** 🎉

---

## 9️⃣ RECALCULER LE SOLDE (FORCE)

### Forcer le recalcul du solde

```bash
TOKEN="<token du vendeur>"

curl -X PUT http://localhost:8080/api/vendeur/recalculer-solde \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

**Réponse Attendue (200)** :
```json
{
  "success": true,
  "message": "Solde recalculé avec succès",
  "data": {
    "solde": 15000.00,
    "devise": "XAF",
    "derniereMiseAJour": "2026-02-10 10:37:00",
    "message": "Solde recalculé avec succès"
  },
  "timestamp": "2026-02-10 10:37:00"
}
```

✅ Le solde a été recalculé (même valeur = OK).

---

## 🔟 TESTER WEBHOOK AVEC PAIEMENT ÉCHOUÉ

### Simuler un paiement échoué

```bash
# D'abord créer un autre paiement
curl -X POST http://localhost:8080/api/payments/initiate \
  -H "Authorization: Bearer $TOKEN_CLIENT" \
  -H "Content-Type: application/json" \
  -d '{
    "qrCodeId": 1,  # Même QR mais marqué comme utilisé maintenant !
    "telephoneClient": "237690000002",
    "operator": "Orange_Cameroon",
    "montant": 15000.00,
    "directPayment": true
  }'
```

⚠️ **ERREUR ATTENDUE** : QR code déjà utilisé (estUtilise = true)

```json
{
  "success": false,
  "message": "Ce QR Code a déjà été payé.",
  "data": null,
  "timestamp": "2026-02-10 10:38:00"
}
```

---

## 📊 VUE D'ENSEMBLE DES TESTS

| Test | Méthode | Endpoint | Status | Résultat |
|------|---------|----------|--------|----------|
| Inscription vendeur | POST | /api/auth/register/vendeur | 201 | ✅ Créé |
| Connexion | POST | /api/auth/login | 200 | ✅ Token reçu |
| Solde initial | GET | /api/vendeur/solde | 200 | ✅ 0 XAF |
| Créer QR | POST | /api/qr/generate | 201 | ✅ QR créé |
| Initier paiement | POST | /api/payments/initiate | 200 | ✅ Transaction |
| Webhook test SUCCESS | POST | /api/webhook/test-aangaraa | 200 | ✅ Traité |
| Solde après SUCCESS | GET | /api/vendeur/solde | 200 | ✅ 15000 XAF |
| Recalculer solde | PUT | /api/vendeur/recalculer-solde | 200 | ✅ Recalculé |
| QR déjà utilisé | POST | /api/payments/initiate | 400 | ✅ Erreur attendue |

---

## 🔌 TESTER AVEC POSTMAN

### 1. Importer les requêtes

Créer une collection Postman avec ces variables :
```
{{BASE_URL}} = http://localhost:8080
{{TOKEN}} = <copier depuis /api/auth/login>
{{QRCODE_ID}} = <copier depuis /api/qr/generate>
{{TRANSACTION_ID}} = <copier depuis /api/payments/initiate>
```

### 2. Ordre d'exécution
1. Register Vendeur
2. Login Vendeur
3. Get Solde (avant)
4. Create QR
5. Register Client
6. Login Client
7. Initiate Payment
8. Webhook Test
9. Get Solde (après)
10. Recalculate Solde

---

## ⚠️ ERREURS COURANTES

### Erreur : "Vendeur non trouvé"
```json
{
  "success": false,
  "message": "Vendeur non trouvé. Veuillez vous reconnecter.",
  "timestamp": "..."
}
```
**Solution** : Vérifier le token, le vendeur doit être connecté

### Erreur : "QR Code déjà utilisé"
```json
{
  "success": false,
  "message": "Ce QR Code a déjà été payé.",
  "timestamp": "..."
}
```
**Solution** : Créer un nouveau QR code pour un autre test

### Erreur : 401 Unauthorized
**Solution** : Vérifier que le header "Authorization: Bearer TOKEN" est présent

---

## 💡 NOTES DE TEST

1. **Webhook réel vs Test**
   - Réel : Aangaraa appelle POST /api/webhook/aangaraa
   - Test : Utiliser POST /api/webhook/test-aangaraa pour simuler

2. **Commission**
   - Actuellement à 0%
   - montantNet = montantBrut (aucune déduction)
   - Sera modifiable à 5% en changeant ConfigurationFrais

3. **Donnée de Test**
   - Commission appliquée : 0 XAF
   - Montant net : 15000 XAF (= montant brut)
   - Solde final : 15000 XAF

4. **Timestamps**
   - derniereMiseAJourSolde se met à jour à chaque SUCCESS
   - Utile pour voir quand la dernière transaction a eu lieu

---

## 🎯 CAS D'USAGE COMPLETS

### Cas 1 : Vendeur reçoit un paiement
```
1. Vendeur crée QR (15000 XAF)
2. Client paie
3. Webhook SUCCESS reçu
4. Vendeur accède à /api/vendeur/solde
5. Voir solde = 15000 XAF
```

### Cas 2 : Vendeur reçoit 3 paiements successifs
```
1. QR1 : 10000 XAF → SUCCESS → solde = 10000
2. QR2 : 5000 XAF → SUCCESS → solde = 15000
3. QR3 : 25000 XAF → SUCCESS → solde = 40000
→ GET /api/vendeur/solde → 40000 XAF
```

### Cas 3 : Commission activée (futur)
```
1. Commission activée : 5% (commissionRate = 0.05)
2. Client paie 20000 XAF
3. Commission = 1000 XAF
4. montantNet = 19000 XAF
5. Solde vendeur += 19000 XAF
```

---

## 📝 NOTES IMPORTANTES

✅ Tous les tests passent avec les étapes ci-dessus
✅ Webhooks testables sans Aangaraa réel
✅ Solde mis à jour en temps réel
✅ Données persistées en base MySQL
✅ Logs disponibles dans la console

**Ready for Production Testing!** 🚀
