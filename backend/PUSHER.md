# Activer Pusher en production

Ce document décrit les étapes minimales pour activer l'envoi de notifications via Pusher en production.

**Résumé**
- Par défaut l'application utilise une implémentation simulée (log + persistance en base).
- Pour activer Pusher en production il faut : ajouter la dépendance Gradle, configurer les propriétés `pusher.*`, réactiver l'implémentation `PusherConfig` et redéployer.

**1) Dépendance Gradle (exemple)**

Dans `build.gradle` (module `backend`) ajoutez la dépendance Pusher. Vérifiez la version disponible sur Maven Central :

```gradle
repositories {
    mavenCentral()
}

dependencies {
    // Remplacez la version par la plus récente disponible sur Maven Central
    implementation 'com.pusher:pusher-http-java:1.8.0'
}
```

Si la résolution échoue, assurez-vous que `mavenCentral()` est présent dans `repositories` et que votre CI/environnement a accès à Internet/aux dépôts Maven.

**2) Propriétés de configuration**

Ajoutez les propriétés de Pusher dans `src/main/resources/application.properties` (ou fournissez-les via des variables d'environnement pour la production) :

```
# Activer ou non Pusher
pusher.enabled=true

# Valeurs à remplir (remplacez par vos identifiants Pusher)
pusher.app-id=YOUR_APP_ID
pusher.key=YOUR_KEY
pusher.secret=YOUR_SECRET
pusher.cluster=YOUR_CLUSTER
```

Recommandation : dans l'environnement de production, utilisez des variables d'environnement pour ces valeurs. Exemple d'association Spring Boot :

```
pusher.app-id=${PUSHER_APP_ID}
pusher.key=${PUSHER_KEY}
pusher.secret=${PUSHER_SECRET}
pusher.cluster=${PUSHER_CLUSTER}
```

**3) Restaurer / activer `PusherConfig`**

Le projet contient un fichier `src/main/java/com/fapshi/backend/config/PusherConfig.java` qui, en développement local, peut être une implémentation « placeholder ». Pour activer Pusher :

- Remplacez/complétez `PusherConfig` pour créer un bean Pusher réel (utiliser le client fourni par la dépendance que vous avez ajoutée).
- Injectez ce bean dans `NotificationService` et appelez le client Pusher pour publier les événements.
- Respectez le flag `pusher.enabled` : si `false`, conservez le fallback de simulation (log + persistance).

Exemple (schématique) dans `PusherConfig` :

```java
@Bean
public Pusher pusher(AppProperties props) {
    if (!props.isPusherEnabled()) return null; // ou un bean no-op
    return new Pusher(props.getAppId(), props.getKey(), props.getSecret(), props.getCluster());
}
```

(adaptez selon le client Java choisi)

**4) Étapes pour déployer et vérifier**

1. Ajouter la dépendance et pousser les changements dans votre branche/CI.
2. Fournir les variables d'environnement `PUSHER_APP_ID`, `PUSHER_KEY`, `PUSHER_SECRET`, `PUSHER_CLUSTER` dans votre environment de déploiement.
3. S'assurer que `pusher.enabled=true` dans les propriétés de production (ou passez la valeur via `--spring.profiles.active`/config map).
4. Rebuild et redéployer l'application :

```bash
# depuis le répertoire backend
./gradlew clean build
# ou pour exécuter
./gradlew bootRun
```

5. Tester via l'endpoint interne : envoyez une notification de test (par ex. `POST /api/notification/send`) et vérifiez :
- que l'événement est reçu dans le tableau de bord Pusher
- que l'application persiste la notification en base
- que les logs montrent l'appel Pusher sans erreurs

**5) Résolution des problèmes courants**

- "Could not find com.pusher:pusher-http-java:1.8.0": vérifiez `mavenCentral()` dans `repositories` et la connectivité réseau du build. Si votre entreprise utilise un proxy ou un Nexus/Artifactory privé, ajoutez la configuration correspondante.
- Problèmes d'authentification : assurez-vous que `app-id`, `key` et `secret` sont corrects et correspondent à votre application Pusher.
- Si vous voulez conserver un fallback en local, conservez `pusher.enabled=false` dans `application.properties` de développement.

**6) Sécurité & bonnes pratiques**

- Ne mettez jamais `pusher.secret` en clair dans le repo. Utilisez des variables d'environnement ou un coffre-fort (Vault, Key Vault, etc.).
- Vérifiez les logs et mettez en place des métriques/alertes sur les échecs d'envoi vers Pusher.

---
Fichiers à éditer pour la réactivation :
- `src/main/java/com/fapshi/backend/config/PusherConfig.java`
- `src/main/java/com/fapshi/backend/service/NotificationService.java`

Si vous voulez, je peux :
- tenter d'ajouter la dépendance et effectuer un build ici pour vérifier la résolution (nécessite accès au dépôt Maven), ou
- générer un exemple concret de `PusherConfig`/modification `NotificationService` prêt à activer dès que la dépendance est résolue.
