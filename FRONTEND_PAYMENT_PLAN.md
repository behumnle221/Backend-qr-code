# Plan d'implémentation frontend

## Objectif
Ce document décrit les flux frontend à implémenter pour utiliser le backend de paiement QR.
Il couvre Flutter et React.js, avec les endpoints, le format des requêtes/réponses, et les workflows à suivre.

---

## 1) Endpoints principaux

### `POST /api/payments/initiate`
- Crée une transaction de paiement.
- Retourne `success`, `message`, `payUrl`, `payToken`, `transactionId`.

### `GET /api/payments/status/{transactionId}`
- Vérifie le statut via le backend / Aangaraa.
- Doit être appelé pour suivre l'évolution d'un paiement externe.

### `GET /api/payments/status/local/{transactionId}`
- Retourne uniquement le statut local stocké en base.
- Utile pour affichage rapide et debugging.

### `GET /api/payments/success`
- Endpoint de redirection Aangaraa.
- Peut être utilisé par un navigateur ou WebView après paiement par redirection. 

---

## 2) Contrat API

### Requête `POST /api/payments/initiate`
```json
{
  "qrCodeId": 123,
  "telephoneClient": "237612345678",
  "operator": "Orange_Cameroon",
  "montant": 1200,
  "directPayment": true,
  "transactionType": "PAYMENT_MARCHAND"
}
```

Champs :
- `qrCodeId` : identifiant du QR code.
- `telephoneClient` : numéro du client payeur.
- `operator` : `Orange_Cameroon` ou `MTN_Cameroon`.
- `montant` : montant à payer.
- `directPayment` : `true` pour paiement direct, `false` pour redirection.
- `transactionType` : `PAYMENT_MARCHAND`, `TRANSFERT_VIRTUEL`, `RECHARGEMENT`, ou `RETRAIT`.

### Réponse `PaymentInitResponse`
```json
{
  "success": true,
  "message": "Paiement initié. Validez sur votre téléphone.",
  "payUrl": "https://...",   
  "payToken": "abc123",
  "transactionId": 42
}
```

Signification :
- `success=true` : la demande a été acceptée.
- `payUrl` présent → paiement par redirection Aangaraa.
- `payToken` peut être utilisé pour suivi.
- `transactionId` doit être conservé pour interroger le statut.

---

## 3) Flux frontend recommandés

### 3.1 Paiement marchand interne
- `transactionType = "PAYMENT_MARCHAND"` ou `"TRANSFERT_VIRTUEL"`
- `telephoneClient` requis.
- `directPayment = true` de préférence.
- Le backend débite le solde client et crédite le vendeur.
- L’UI doit afficher le résultat immédiatement.

### 3.2 Paiement Aangaraa direct
- `transactionType = "RECHARGEMENT"` ou `"RETRAIT"`
- `directPayment = true`
- Le frontend doit appeler `GET /api/payments/status/{transactionId}` en boucle jusqu’à réussite ou échec.

### 3.3 Paiement par redirection
- `transactionType = "RECHARGEMENT"` ou `"RETRAIT"`
- `directPayment = false`
- Ouvrir `payUrl` dans WebView ou navigateur.
- Après retour, vérifier le statut avec `GET /api/payments/status/{transactionId}`.

---

## 4) Données frontend à gérer

### Requête de paiement
- `qrCodeId`
- `telephoneClient`
- `operator`
- `montant`
- `directPayment`
- `transactionType`

### Réponse attendue
- `success`
- `message`
- `payUrl`
- `payToken`
- `transactionId`

### Statut
- `status` côté backend peut être : `PENDING`, `SUCCESS`, `SUCCESSFUL`, `FAILED`, etc.
- `success` boolean : aider l’affichage.

---

## 5) Plan Flutter

### 5.1 Services à créer
- `PaymentService` ou `PaymentApi`
- méthodes :
  - `Future<PaymentInitResponse> initiatePayment(...)`
  - `Future<Map<String, dynamic>> checkPaymentStatus(int transactionId)`
  - `Future<Map<String, dynamic>> getLocalPaymentStatus(int transactionId)`

### 5.2 Modèles à définir
- `InitiatePaymentRequest`
- `PaymentInitResponse`

### 5.3 Composants / écrans
- `PaymentFormScreen`
  - saisie montant, opérateur, téléphone client
  - bouton `Payer`
- `PaymentResultScreen`
  - affiche `success`, `message`, `transactionId`
  - bouton `Ouvrir payUrl` si nécessaire
- `PaymentStatusScreen`
  - poll du statut avec `GET /api/payments/status/{transactionId}`
  - rafraîchissement manuel

### 5.4 Workflow Flutter
1. L’utilisateur saisit le montant et le numéro.
2. Appel `POST /api/payments/initiate`.
3. Si réponse contient `payUrl`, ouvrir WebView.
4. Si paiement interne ou direct, afficher le résultat.
5. Pour les paiements Aangaraa, appeler `GET /api/payments/status/{transactionId}` jusqu’à succès ou échec.

---

## 6) Plan React.js

### 6.1 Services à créer
- `paymentService.js`
- fonctions :
  - `initiatePayment(requestBody)`
  - `getPaymentStatus(transactionId)`
  - `getLocalPaymentStatus(transactionId)`

### 6.2 Pages / composants
- `PaymentForm` : formulaire de paiement.
- `PaymentResult` : affichage de la réponse du backend.
- `PaymentStatus` : suivi du paiement en temps réel.

### 6.3 Workflow React
1. Créer la requête de paiement.
2. Appeler `POST /api/payments/initiate`.
3. Si `payUrl` existe, ouvrir un nouvel onglet ou rediriger.
4. Vérifier le statut avec `GET /api/payments/status/{transactionId}`.
5. Afficher un message clair en cas de succès ou d’échec.

---

## 7) Recommandations pour l’équipe frontend

- Toujours conserver `transactionId`.
- Vérifier `success` et `message` avant d’afficher.
- Gérer les cas de paiement externe avec polling.
- Tester séparément :
  - paiement interne (`PAYMENT_MARCHAND`)
  - paiement externe Aangaraa direct
  - paiement externe par redirection
- Utiliser `transactionType` pour forcer le flux métier.

---

## 8) Exemple de valeurs de `transactionType`
- `PAYMENT_MARCHAND`
- `TRANSFERT_VIRTUEL`
- `RECHARGEMENT`
- `RETRAIT`

---

## 9) Notes importantes

- Si le QR code n’a pas de `usageType`, le backend choisit un flux par défaut.
- Le webhook Aangaraa est géré côté backend et ne doit pas être exposé directement par le frontend.
- Pour un affichage immédiat, utiliser `GET /api/payments/status/local/{transactionId}` avant de poller l’API externe.
