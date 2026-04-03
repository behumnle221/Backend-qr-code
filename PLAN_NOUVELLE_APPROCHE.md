# Plan de mise en œuvre de la nouvelle approche

## Objectifs
Modifier le système de paiement pour que :
1. Les paiements QR marchands soient traités en interne, sans passer par Aangaraa.
2. Les rechargements et retraits restent gérés par Aangaraa.
3. Les comptes virtuels internes soient utilisés pour les transferts client → commerçant.
4. Le QR code contienne l’identifiant du commerçant.
5. Chaque transaction soit tracée de manière complète.
6. Les soldes virtuels soient protégés contre les conditions de course.

## Nouveau flux cible

### Paiement QR Code marchand
- Le client scanne le QR code.
- Le backend détecte qu’il s’agit d’un paiement marchand.
- Le backend vérifie le QR code et le commerçant.
- Le backend effectue un débit atomique sur le compte client.
- Le backend effectue un crédit sur le compte vendeur.
- Le backend enregistre une transaction interne `TRANSFERT_VIRTUEL`.
- Le QR code est marqué comme utilisé.
- Le client reçoit un succès immédiat.

### Rechargement / Retrait
- Le client demande un rechargement ou un retrait.
- Le backend prépare un payload pour Aangaraa.
- Aangaraa traite le paiement mobile / money.
- Aangaraa notifie le backend via webhook.
- Le backend met à jour le statut de la transaction.
- En cas de succès, le solde virtuel est ajusté.

## Analyse de l’état actuel et des écarts

### Ce qui est déjà en place
- `QRCode` possède déjà une relation vers `Vendeur`.
- Le service de paiement existe avec un flux Aangaraa cohérent.
- `VendeurService` gère un `soldeVirtuel` et des transactions liées.

### Ce qui manque / doit changer
- `Transaction` n’a pas de type explicite pour distinguer les flux.
- Le paiement QR marchand passe encore par Aangaraa.
- Il n’y a pas de solde client atomique ou de champ de solde client.
- La traçabilité détaillée manque (qui, quoi, quand, pourquoi, résultat).
- Les opérations de débit/crédit ne sont pas sécurisées contre les conditions de course.

## Plan d’action concret

### Phase 1 : Modèle de données
1. `Transaction.java`
   - ajouter un champ `transactionType` ou `type` avec un enum :
     - `PAYMENT_MARCHAND`
     - `RECHARGEMENT`
     - `RETRAIT`
     - `TRANSFERT_VIRTUEL`
   - ajouter les données de traçabilité : initérateur, bénéficiaire, description/motif, montant, statut, timestamps.

2. `QRCode.java`
   - s’assurer que le QR code contient bien le lien vers le commerçant.
   - éventuellement ajouter un indicateur de type d’usage QR.

3. `Client.java` / `User.java`
   - ajouter un champ de solde virtuel pour le client ou une entité `CompteVirtuel` associée.

4. `AuditLog.java` (optionnel mais recommandé)
   - ajouter une entité dédiée si le modèle actuel n’est pas suffisant pour tracer chaque opération critique.

### Phase 2 : Repositories et opérations atomiques
1. `ClientRepository.java`
   - ajouter une méthode `debiterSiSuffisant(...)` avec `@Modifying` et `@Transactional`.

2. `VendeurRepository.java`
   - ajouter une méthode `crediterVendeur(...)` atomique.

3. `TransactionRepository.java`
   - ajouter des méthodes de recherche par type, par statut, par vendeur, par client, par token.

4. `AuditLogRepository.java`
   - si audit ajouté, prévoir les méthodes de sauvegarde et de consultation.

### Phase 3 : Logique métier dans les services
1. `PaymentService.java`
   - détecter le type de transaction dès l’initiation.
   - pour `PAYMENT_MARCHAND` :
     - valider le QR code et le vendeur.
     - vérifier et débiter le client atomiquement.
     - créditer le vendeur.
     - créer une transaction interne `TRANSFERT_VIRTUEL`.
     - marquer le QR code comme utilisé.
     - retourner un succès immédiat.
   - pour `RECHARGEMENT` / `RETRAIT` :
     - garder le flux Aangaraa existant.
     - utiliser le webhook et le scheduler pour la confirmation.

2. `VendeurService.java`
   - ajouter des méthodes de crédit/débit transactionnelles.
   - mettre à jour le solde virtuel à chaque opération.

3. `ClientService.java`
   - exposer le solde virtuel client.
   - implémenter le débit atomique.
   - valider le solde avant débit.

4. Concurrence / sécurité
   - utiliser `@Transactional` sur les transferts.
   - éviter les soldes négatifs.
   - garantir que `debiterSiSuffisant` et `crediterVendeur` sont atomiques.

### Phase 4 : Contrôleurs
1. `PaymentController.java`
   - séparer les endpoints ou paramètres selon le type de paiement.
   - autoriser l’initiation de paiement marchand interne.
   - conserver les endpoints de vérification de statut.

2. `QRCodeController.java`
   - générer des QR codes avec l’ID du commerçant.
   - vérifier que le QR code est bien utilisable pour un paiement marchand.

### Phase 5 : Traçabilité et audit
1. Traçabilité complète dans `Transaction` ou `AuditLog` :
   - Qui a initié l’opération.
   - Quel type d’opération.
   - Quand elle a été faite.
   - Pour quoi (QR code, description, montant).
   - Résultat final (succès/échec, solde avant/après).

2. Logs immuables et consultables.
3. Séparer les logs internes des logs Aangaraa.

### Phase 6 : Tests
1. Paiement marchand réussi.
2. Paiement marchand échoué (fonds insuffisants).
3. Rechargement via Aangaraa.
4. Retrait via Aangaraa.
5. QR code invalide/expiré/déjà utilisé.
6. Vérification de la traçabilité.
7. Tests de concurrence pour les soldes.

## Diagramme de flux proposé

```
Paiement QR Code Marchand:
Client -> [Scan QR Code contenant ID Commerçant]
        -> Backend (détecte type PAYMENT_MARCHAND)
        -> Backend (vérifie le QR code et le solde client)
        -> Backend (débit client atomique + audit)
        -> Backend (crédit vendeur + audit)
        -> Backend (marque QR utilisé, crée transaction TRANSFERT_VIRTUEL)
        -> Backend (retour succès immédiat)
        -> Commerçant (notification crédit)
```

```
Rechargement / Retrait:
Client -> [Demande de rechargement/retrait]
        -> Backend (détecte type RECHARGEMENT/RETRAIT)
        -> Backend (prépare payload pour Aangaraa)
        -> Backend (appel API Aangaraa)
        -> Aangaraa (traitement)
        -> Aangaraa (webhook vers backend)
        -> Backend (mise à jour statut transaction + audit)
        -> Backend (ajuste le solde virtuel si succès)
```

## Fichiers à modifier
1. `backend/src/main/java/com/fapshi/backend/entity/QRCode.java`
2. `backend/src/main/java/com/fapshi/backend/entity/Transaction.java`
3. `backend/src/main/java/com/fapshi/backend/entity/Client.java` ou `User.java`
4. `backend/src/main/java/com/fapshi/backend/entity/AuditLog.java` (si nécessaire)
5. `backend/src/main/java/com/fapshi/backend/service/PaymentService.java`
6. `backend/src/main/java/com/fapshi/backend/service/VendeurService.java`
7. `backend/src/main/java/com/fapshi/backend/service/ClientService.java`
8. `backend/src/main/java/com/fapshi/backend/controller/PaymentController.java`
9. `backend/src/main/java/com/fapshi/backend/controller/QRCodeController.java`
10. `backend/src/main/java/com/fapshi/backend/repository/ClientRepository.java`
11. `backend/src/main/java/com/fapshi/backend/repository/VendeurRepository.java`
12. `backend/src/main/java/com/fapshi/backend/repository/TransactionRepository.java`
13. `backend/src/main/java/com/fapshi/backend/repository/AuditLogRepository.java` (si besoin)
14. `backend/src/main/java/com/fapshi/backend/dto/request/InitiatePaymentRequest.java`

## Considérations techniques
- Toutes les opérations de débit/crédit doivent être transactionnelles.
- Utiliser des requêtes atomiques pour les soldes.
- Gérer proprement les erreurs et rollback.
- Contrôler les soldes négatifs.
- Préserver la compatibilité avec le schéma existant.
- Conserver Aangaraa uniquement pour cash-in / cash-out.
- Vérifier que les logs sont exploitables et immuables.

## Résumé
Cette approche permet de :
- simplifier les paiements QR marchands,
- réduire la dépendance à Aangaraa pour les flux internes,
- garder Aangaraa pour les changements de cash,
- améliorer la traçabilité,
- sécuriser les soldes virtuels.

La prochaine étape est de transformer ce plan en changements concrets dans le code, en commençant par les entités et les repositories.
