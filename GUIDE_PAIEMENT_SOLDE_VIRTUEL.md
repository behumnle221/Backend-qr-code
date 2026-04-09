# 📋 Guide de Test - Paiement par Solde Virtuel (Simplifié)

## ✨ Nouveau Endpoint Créé

**Endpoint** : `POST /api/payments/virtual`

Ce nouvel endpoint simplifie les paiements internes par solde virtuel en **ne demandant que 2 champs** :

```json
{
  "qrCodeId": 1,
  "montant": 10
}
```

---

## 🔐 Authentification Requise

L'endpoint est **authentifié** (JWT Token) et récupère automatiquement :
- ✅ L'ID du client depuis le JWT
- ✅ Le numéro de téléphone du client depuis la BDD
- ✅ Le type de transaction (TRANSFERT_VIRTUEL)

Tu n'as **PAS besoin** de fournir :
- ❌ `telephoneClient` (récupéré automatiquement)
- ❌ `operator` (pas d'agrégateur pour interne)
- ❌ `directPayment` (pas pertinent)
- ❌ `transactionType` (déterminé auto)

---

## 📝 Flux De Test Complet

### Étape 1 : Créer un Client avec Solde Virtuel

**Endpoint** : `POST /api/auth/register/client`

```json
{
  "nom": "Jeremie Test",
  "email": "jeremie@test.com",
  "telephone": "+237670000000",
  "password": "password123"
}
```

**Réponse** : Récupère l'ID du client (exemple: `1`)

---

### Étape 2 : Créditer le Solde Virtuel du Client (Simulation)

**Endpoint** : `POST /api/client/recharger` (avec JWT du client)

```json
{
  "montant": 100,
  "operator": "Orange_Cameroon",
  "telephone": "+237670000000"
}
```

**Résultat** : Solde du client = 100 XAF

---

### Étape 3 : Créer un Vendeur

**Endpoint** : `POST /api/auth/register/vendeur`

```json
{
  "nomCommerce": "Shop Test",
  "email": "vendeur@test.com",
  "telephone": "+237670111111",
  "password": "password123"
}
```

**Réponse** : Récupère l'ID du vendeur (exemple: `1`)

---

### Étape 4 : Générer un QR Code (Vendeur)

**Endpoint** : `POST /api/qr/generate` (avec JWT du vendeur)

```json
{
  "products": [
    {
      "name": "Produit Test",
      "quantity": 1,
      "price": 10
    }
  ],
  "description": "Achat test",
  "dateExpiration": "2026-12-31T23:59:59"
}
```

**Réponse** : Récupère l'ID du QR code (exemple: `1`)

```json
{
  "success": true,
  "message": "QR Code généré avec succès",
  "qrCodeId": 1,
  "qrCodeData": "..."
}
```

---

### Étape 5 : Effectuer le Paiement par Solde Virtuel (Client)

**Endpoint** : `POST /api/payments/virtual` (avec JWT du client)

```json
{
  "qrCodeId": 1,
  "montant": 10
}
```

**Réponse** : Succès

```json
{
  "success": true,
  "message": "Paiement par solde virtuel effectué avec succès",
  "transactionId": 123
}
```

**Résultats** :
- ✅ Solde client : 100 - 10 = **90 XAF**
- ✅ Solde vendeur : + 10 = **10 XAF**
- ✅ Transaction créée avec statut : **SUCCESS**
- ✅ QR code marqué comme utilisé

---

### Étape 6 : Vérifier le Solde du Client

**Endpoint** : `GET /api/client/solde` (avec JWT du client)

**Réponse** : `90`

---

### Étape 7 : Vérifier les Transactions du Client

**Endpoint** : `GET /api/client/transactions` (avec JWT du client)

**Réponse** :
```json
{
  "content": [
    {
      "id": 123,
      "transactionId": "TRANS_1704067200000_1234",
      "montant": 10,
      "statut": "SUCCESS",
      "transactionType": "TRANSFERT_VIRTUEL",
      "dateCreation": "2026-04-09T...",
      "telephoneClient": "+237670000000"
    }
  ],
  "totalElements": 1
}
```

---

## 🧪 Commande cURL Complète (Test)

```bash
# 1. Récupérer le JWT du client
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "jeremie@test.com",
    "password": "password123"
  }' | jq -r '.data.token')

# 2. Effectuer le paiement virtuel
curl -X POST http://localhost:8080/api/payments/virtual \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "qrCodeId": 1,
    "montant": 10
  }'
```

---

## ✅ Points Clés

| Champ | Avant | Après | Notes |
|-------|-------|-------|-------|
| `qrCodeId` | Obligatoire | Obligatoire | À fournir |
| `montant` | Obligatoire | Obligatoire | À fournir |
| `telephoneClient` | Obligatoire | Récupéré auto | Du JWT |
| `operator` | Obligatoire | Non utilisé | Paiement interne |
| `directPayment` | Obligatoire | Non utilisé | Paiement interne |
| `transactionType` | À spécifier | Déterminé auto | Toujours TRANSFERT_VIRTUEL |

**Simplification** : **De 6 champs requis → 2 champs requis**

---

## 🐛 Gestion des Erreurs

**Erreur : Client non trouvé**
```json
{
  "error": "Client non trouvé"
}
```
→ Vérifier que le JWT est valide

**Erreur : Solde insuffisant**
```json
{
  "error": "Solde insuffisant. Solde: 5 XAF, Montant demandé: 10 XAF"
}
```
→ Recharger le compte du client

**Erreur : QR code déjà payé**
```json
{
  "error": "QR Code déjà payé"
}
```
→ Générer un nouveau QR code

**Erreur : QR code expiré**
```json
{
  "error": "QR Code expiré"
}
```
→ Générer un nouveau QR code avec dateExpiration future
