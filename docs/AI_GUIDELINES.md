# Guide de bonnes pratiques : l'IA dans le cycle de développement

Document de travail, à adapter par chaque équipe. Il décrit comment TestPilot applique ces règles et comment les étendre à l'usage quotidien d'assistants de code (Copilot, Gemini, etc.).

## 1. Principe directeur

**L'IA propose, le développeur décide et reste responsable.** Tout code ou test produit par une IA est traité comme du code écrit par un junior : il est relu, exécuté et compris avant d'être intégré.

## 2. Données : ce qui ne doit jamais quitter l'entreprise

| Interdit dans un prompt | Exemples |
|---|---|
| Secrets et identifiants | mots de passe, clés API, jetons, chaînes de connexion |
| Données personnelles | emails, noms de clients, IBAN, numéros de téléphone |
| Code ou données classés confidentiels | modules critiques, jeux de données de production |

**Dans TestPilot :** `SensitiveDataRedactor` masque automatiquement ces éléments avant toute génération, et le générateur ne reçoit que le code masqué (vérifié par un test automatisé). Le nombre de masquages est affiché dans le tableau de bord, car un nombre élevé signale un problème de pratique à traiter en équipe (secrets en dur dans le code).

Limite assumée : le masquage par motifs ne remplace pas la vigilance. Il attrape les cas courants, pas tous les cas.

## 3. Validation humaine obligatoire

- Aucune suggestion n'est « valide » tant qu'un humain ne l'a pas approuvée (statuts `PROPOSED`, `APPROVED`, `REJECTED`).
- Une suggestion relue ne peut pas être relue une seconde fois (traçabilité, pas de revirement silencieux).
- Un test généré doit **échouer quand on casse volontairement le code** : sinon il ne protège rien. Vérifiez-le systématiquement.
- Les squelettes contiennent des `TODO` volontaires : l'assertion attendue est une décision métier, pas un choix de l'outil.

## 4. Qualité

- Les tests générés suivent les mêmes règles que les autres : nommage clair, une raison d'échouer par test, motif Arrange / Act / Assert, aucun test dépendant de l'ordre d'exécution.
- La couverture est un seuil minimal, pas un objectif : elle est vérifiée en CI (JaCoCo côté Java, Vitest côté React) et bloque le merge en dessous du seuil.
- Une couverture élevée ne prouve rien si les assertions sont faibles. Pour les modules critiques, compléter par du test de mutation (PIT).

## 5. Sécurité, licences, propriété intellectuelle

- Relire le code généré comme du code tiers : dépendances inconnues, appels réseau, chiffrement maison sont à refuser.
- Ne pas coller du code sous licence incompatible dans un prompt, ne pas intégrer de code généré dont l'origine est douteuse.
- N'utiliser que les outils validés par l'entreprise (contrat, hébergement, politique de rétention des données).

## 6. Mesurer pour piloter

Indicateurs suivis par TestPilot (`GET /api/stats`) :

- **taux d'acceptation** des suggestions (approuvées / relues) : si trop bas, l'outil ne sert à rien ; si proche de 100 %, la relecture est peut-être trop légère ;
- nombre de suggestions, relues ou non ;
- nombre de secrets masqués.

À suivre en parallèle côté équipe : temps passé à écrire des tests, bugs détectés en recette, durée des revues de code, retour des développeurs.

## 7. Adoption et accompagnement

1. Commencer par des cas d'usage à faible risque : génération de squelettes de tests, documentation, explication de code.
2. Faire des ateliers courts sur du code réel de l'équipe, pas sur des exemples de démonstration.
3. Partager les bons et les mauvais prompts dans un dépôt commun.
4. Revoir ce guide à chaque retour d'expérience (rétrospective Scrum).

## 8. Checklist de revue d'une merge request contenant du code assisté par IA

- [ ] Je comprends chaque ligne ajoutée.
- [ ] Aucun secret ni donnée personnelle n'a été envoyé à l'outil.
- [ ] Les tests passent, et échouent si je casse le code.
- [ ] Aucune dépendance ni licence nouvelle non vérifiée.
- [ ] Les cas limites et les erreurs sont testés, pas seulement le cas nominal.
