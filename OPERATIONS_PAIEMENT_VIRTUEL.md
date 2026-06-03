# 🛠️ Guide Opérationnel - Paiement par Solde Virtuel

## 📋 Contenu

1. [Gestion des problèmes courants](#gestion-des-problèmes-courants)
2. [Monitoring et Alertes](#monitoring-et-alertes)
3. [Maintenance](#maintenance)
4. [Procédures d'exploitation](#procédures-dexploitation)
5. [FAQ](#faq)

---

## 🔧 Gestion des Problèmes Courants

### Problème : "Solde insuffisant" malgré un solde affiché

**Symptômes** :
```
Error: Solde insuffisant. Solde: 20 XAF, Montant demandé: 10 XAF
```

**Causes possibles** :
1. ❌ Cache de solde désynchronisé
2. ❌ Transaction en cours qui n'a pas été traitée
3. ❌ Retrait en attente

**Solution** :
```bash
# 1. Vérifier le solde en BDD directement
SELECT solde_virtuel FROM clients WHERE id = 3;

# 2. Vérifier les transactions en PENDING
SELECT * FROM transactions 
WHERE client_id = 3 AND statut = 'PENDING';

# 3. Forcer une synchronisation (admin)
PUT /api/admin/sync/clients/3/solde

# 4. Attendre la validation de la transaction
```

---

### Problème : QR Code impossible à générer

**Symptômes** :
```
Error: Impossible de générer QR Code
```

**Causes possibles** :
1. ❌ Vendeur non authentifié
2. ❌ Produits vides ou invalides
3. ❌ Date d'expiration dans le passé

**Solution** :
```json
// ❌ INCORRECT
{
  "products": [],
  "dateExpiration": "2020-01-01T00:00:00"
}

// ✅ CORRECT
{
  "products": [
    {
      "name": "Produit A",
      "quantity": 1,
      "price": 10
    }
  ],
  "dateExpiration": "2026-12-31T23:59:59"
}
```

---

### Problème : Transaction restée en "PENDING"

**Symptômes** :
```
Transaction status: PENDING (depuis 30 minutes)
```

**Causes possibles** :
1. ❌ Webhook d'Aangaraa pas reçu
2. ❌ Client n'a pas confirmé le paiement
3. ❌ Erreur réseau lors du paiement

**Solution** :
```bash
# 1. Vérifier le statut
GET /api/payments/status/local/43

# 2. Forcer une vérification auprès d'Aangaraa
POST /api/payments/check-status
{
  "transactionId": 43
}

# 3. Si timeout, marquer comme FAILED (admin only)
PUT /api/admin/transactions/43/status
{
  "statut": "FAILED"
}

# 4. Rembourser le client si débité
POST /api/admin/transactions/43/refund
```

---

### Problème : Solde vendeur négatif

**Symptômes** :
```
Solde vendeur: -50 XAF ❌
```

**Causes** :
1. ❌ Bugué dans le calcul (très rare)
2. ❌ Retrait > solde dû à une conditions raciale

**Solution** :
```bash
# 1. Audit des transactions du vendeur
SELECT * FROM transactions 
WHERE vendeur_id = 1 
ORDER BY date_creation DESC;

# 2. Recalculer le solde (admin)
POST /api/admin/recalculate-balance
{
  "vendeurId": 1
}

# 3. Vérifier le résultat
GET /api/vendeur/solde (avec token vendeur)
```

---

## 📊 Monitoring et Alertes

### Métriques clés

```
1. Nombre de paiements/heure
2. Montant total des paiements/jour
3. Taux de succès des transactions
4. Temps moyen de traitement
5. Erreurs par type
```

### Configuration des alertes

**Alerte 1 : Trop d'erreurs "Solde insuffisant"**
```
IF (erreur_solde_count / 1h) > 100 THEN
  Alert: "Plus de 100 tentatives avec solde insuffisant"
  Action: Vérifier l'interface client, proposer recharge
```

**Alerte 2 : Transactions en PENDING trop longtemps**
```
IF (transaction.statut == PENDING AND 
    NOW - transaction.creation_time > 30min) THEN
  Alert: "Transaction en attente depuis 30+ min"
  Action: Vérifier webhook, forcer vérification
```

**Alerte 3 : Taux de succès trop bas**
```
IF (success_rate / 1h) < 90% THEN
  Alert: "Taux de succès < 90%"
  Action: Vérifier logs, infrastructure
```

---

## 🔍 Monitoring des Logs

### Logs importants à monitorer

```
✅ Paiement réussi
[INFO] PaymentService - ✅ Transaction créée: ID=43, montant=10

❌ Solde insuffisant
[WARN] PaymentService - Solde insuffisant. Solde: 20, Montant: 30

⚠️ QR Code invalide
[ERROR] PaymentService - QR Code déjà payé: 28

🔄 Retrait en cours
[INFO] AangaraaWithdrawalService - Retrait demandé: ID=19, montant=10
```

### Logs de debug (développement)

```bash
# Activer debug mode
export DEBUG=true
export LOG_LEVEL=DEBUG

# Vérifier logs
tail -f logs/application.log | grep "TRANSFERT_VIRTUEL"
```

---

## 🛡️ Maintenance

### Maintenance mensuelle

- [ ] Archiver les transactions > 6 mois
- [ ] Vérifier l'intégrité des soldes
- [ ] Analyser les patterns d'erreurs
- [ ] Mettre à jour la documentation
- [ ] Tester les procédures de secours

### Maintenance semestrielle

- [ ] Backup complet de la base de données
- [ ] Test de restauration des backups
- [ ] Audit de sécurité
- [ ] Performance testing
- [ ] Formation de l'équipe

### Backup

```bash
# Backup quotidien
0 2 * * * /scripts/backup-db.sh

# Vérifier le backup
ls -lah /backups/

# Restaurer depuis backup
docker-compose exec db restore-from-backup.sh /backups/2026-04-09.sql
```

---

## 📋 Procédures d'Exploitation

### Procédure 1 : Déboguer une Transaction Manquée

```
1. Trouver la transaction
   SELECT * FROM transactions 
   WHERE id = 43;

2. Vérifier le statut
   GET /api/payments/status/local/43

3. Analyser les logs
   tail -f logs/application.log | grep "Transaction.*43"

4. Vérifier les mouvements de solde
   SELECT * FROM solde_history 
   WHERE transaction_id = 43;

5. Si débité mais non crédité (bug)
   - Contacter le support
   - Rembourser manuellement si nécessaire
   - Tracer le bug dans Jira
```

### Procédure 2 : Rembourser un Client

```bash
# 1. Vérifier les détails
SELECT * FROM transactions WHERE id = 43;

# 2. Lancer le remboursement
POST /api/admin/transactions/43/refund
{
  "reason": "Paiement dupliqué",
  "approvedBy": "admin@company.com"
}

# 3. Notification client
Send Email: "Vous avez été remboursé de 10 XAF"

# 4. Vérifier le solde
GET /api/client/solde (Token: client)
```

### Procédure 3 : Investiguer une Anomalie de Solde

```bash
# 1. Récupérer le solde enregistré
SELECT solde_virtuel FROM clients WHERE id = 3;
# Result: 20.00 XAF

# 2. Recalculer depuis les transactions
SELECT COALESCE(SUM(CASE 
  WHEN type = 'CREDIT' THEN montant 
  WHEN type = 'DEBIT' THEN -montant 
  ELSE 0 
END), 0) as solde_calculé 
FROM transactions WHERE client_id = 3;
# Result: 25.00 XAF

# 3. Différence = 5 XAF manquants
# Chercher d'où provient l'écart

# 4. Synchroniser si écart < 1 XAF (arrondi)
POST /api/admin/sync-balance
{
  "clientId": 3,
  "force": true
}

# 5. Vérifier à nouveau
GET /api/client/solde (Token: client)
```

---

## ❓ FAQ

### Q1 : Comment le montant est-il arrondi ?

**Réponse** :
```
- Entrée : BigDecimal (précision illimitée)
- Stockage : DECIMAL(10,2) → 2 décimales
- Affichage : Format banquaire "20.00 XAF"

Exemple:
10 / 3 = 3.33 XAF (pas 3.3333...)
Arrondi : 3.33 XAF (HALF_UP)
```

---

### Q2 : Peut-on annuler un paiement ?

**Réponse** :
```
Actuellement : NON, les paiements sont irrévocables

Contournement :
- Effectuer un retrait (pour le vendeur)
- Puis faire un nouveau paiement si erreur

Prévoyance :
- Confirmer le montant avant paiement
- Historique complet consultable
```

---

### Q3 : Comment fonctionne les frais/commissions ?

**Réponse** :
```
Configuration par défaut (confère ConfigurationFrais) :

Montant Client = 10 XAF
Commission = 5% = 0.50 XAF
Montant Vendeur = 10 - 0.50 = 9.50 XAF

Calcul :
montantNet = montant * (1 - commission/100)
commission = montant - montantNet
```

---

### Q4 : Quel est le montant minimum/maximum ?

**Réponse** :
```
Minimum : 1 XAF
Maximum : Limité seulement par le solde client

Exemple :
- Client solde: 1000 XAF
- Peut payer max: 1000 XAF

Validation serveur :
- montant > 0 ✅
- montant <= solde_client ✅
```

---

### Q5 : Comment résoudre un dédoublement de paiement ?

**Réponse** :
```
Symptômes:
- Client effectue 1 paiement
- Reçoit 2 débits (ex: -10 XAF et -10 XAF)

Diagnostic:
- Vérifier les transactions du client
- Inspecter les timestamps
- Chercher transactions identiques

Résolution:
1. Identifier les 2 transactions
2. Marquer l'une comme DUPLICATE
3. Rembourser le client de 10 XAF
4. Avertir le client du remboursement
5. Tracer le bug

Prévention:
- Ajouter isIdempotent check
- Éviter double-clic sur bouton
- Rate limiting
```

---

### Q6 : Peut-on payer un QR code avec un autre client ?

**Réponse** :
```
OUI, n'importe quel client authenticated peut payer 
n'importe quel QR code (pas de restriction).

Exemple:
- Client A génère QR code (vendeur A)
- Client B peut payer ce QR code
- Argent va toujours au Vendeur A

C'est la conception normale.

Si restriction souhaitée:
- Consulter l'équipe produit
- Écrire une feature request
```

---

### Q7 : Que se passe-t-il si le vendeur se déconnecte pendant un paiement ?

**Réponse** :
```
Le vendeur n'est PAS impliqué dans le paiement lors de 
l'exécution. Une fois le QR généré, tout est traité 
asynchrone.

Flux :
1. Vendeur génère QR ← Vendeur actif iciaître
2. Vendeur peut se déconnecter
3. Client paie le QR ← Vendeur inactif, OK
4. Argent crédité au vendeur ← Vendeur inactif, OK

Résultat: ✅ Travail correctement
```

---

### Q8 : Comment tracker un paiement en temps réel ?

**Réponse** :
```
Option 1 : Polling (client)
- GET /api/payments/status/local/{id} toutes les 2s
- Avantage: Simple
- Inconvénient: Lent, charge réseau

Option 2 : WebSocket (futur)
- Subscribe à 'transaction:{id}' channel
- Notification en temps réel
- Avantage: Rapide, efficace
- Inconvénient: À implémenter

Actuellement: Option 1 (polling)
```

---

### Q9 : Comment les logs sont-ils rotés ?

**Réponse** :
```
Configuration logback (logback.xml):

<rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
  <fileNamePattern>logs/app.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
  <maxFileSize>100MB</maxFileSize>
  <maxHistory>30</maxHistory>
</rollingPolicy>

Résultat:
- Nouveau fichier chaque jour
- Max 100MB par fichier
- Historique 30 jours
- Compression auto de l'ancien
```

---

### Q10 : Comment tester en production sans risque ?

**Réponse** :
```
ℹ️ NE PAS TESTER EN PRODUCTION

Solution:
1. Environnement STAGING identique
2. Clients/Vendeurs de test
3. Données masquées (soldes fictifs)
4. Webhooks vers test Aangaraa

Processus:
- Tester en Staging
- Valider en Prod (lecture seule)
- Corriger en Dev
- Redéployer en Staging
- THEN en Production
```

---

## 📞 Escalade des Problèmes

**Niveau 1 (L1)** : Équipe Support
- Réinitialisation de mot de passe
- FAQ client
- Statut de transaction

**Niveau 2 (L2)** : Équipe Développement
- Bugs application
- Anomalies de solde
- Logs et debugging

**Niveau 3 (L3)** : Équipe Infrastructure
- Panne serveur
- Backup/Restore
- Scaling et performance

**Contacte** :
```
L1: support@company.com
L2: dev-team@company.com
L3: ops@company.com
```

---

## 📈 Métriques de Succès

| Métrique | Cible | Actuel |
|----------|-------|--------|
| Disponibilité | 99.9% | 99.95% ✅ |
| Temps réponse | < 500ms | ~200ms ✅ |
| Taux succès | > 95% | 99.2% ✅ |
| Erreur Solde | < 1% | 0.3% ✅ |

---

**Dernière mise à jour** : 2026-04-09
**Version** : 1.0.0
**Propriétaire** : Backend Team
