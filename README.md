# TestPilot

Outil de **suggestion de tests unitaires assistée par IA, avec validation humaine**.
Backend Java 21 / Spring Boot 3, frontend React 18 / TypeScript, CI GitLab.

> L'IA propose, un humain valide. Les secrets sont masqués avant toute génération.

## Ce que fait le projet

1. Vous collez une classe Java dans l'interface.
2. **Masquage** des données sensibles (mots de passe, clés API, jetons, IBAN, emails) avant tout traitement.
3. **Analyse** du code : nom de classe et méthodes publiques.
4. **Génération** d'un squelette JUnit 5 par méthode (cas nominal + cas limite, motif Arrange / Act / Assert).
5. **Revue humaine** : approuver ou rejeter chaque suggestion (une seule revue par suggestion).
6. **Indicateurs** : nombre de suggestions, taux d'acceptation, secrets masqués.

## Architecture

```
frontend (React + Vite)  --/api-->  backend (Spring Boot)
                                      ├─ api/         contrôleur REST, erreurs RFC 7807
                                      ├─ service/     orchestration + règles de revue
                                      ├─ analysis/    masquage des secrets, parseur Java
                                      ├─ generation/  interface TestGenerator + générateur à gabarits
                                      └─ domain/      records immuables
```

Choix de conception :

- `TestGenerator` est une **interface** : le générateur livré (`TemplateTestGenerator`) est déterministe et hors-ligne. Une implémentation adossée à un LLM (Copilot, Gemini, Claude…) se branche sans toucher au reste. Le générateur ne reçoit que le **code déjà masqué**, et un test automatisé le vérifie.
- Stockage en mémoire (`SuggestionRepository`) : remplaçable par JPA ou MongoDB.
- `Clock` injecté : tests déterministes.

## Lancer en local

Prérequis : Java 21, Maven 3.9, Node 22.

```bash
# Backend (http://localhost:8080)
cd backend && mvn spring-boot:run

# Frontend (http://localhost:5173, proxy /api vers le backend)
cd frontend && npm install && npm run dev
```

Ou avec Docker : `docker compose up --build` puis http://localhost:8081.

### API

| Méthode | Route | Rôle |
|---|---|---|
| POST | `/api/analyses` | `{ "source": "..." }` : crée une suggestion par méthode publique |
| GET | `/api/suggestions` | liste des suggestions, les plus récentes d'abord |
| POST | `/api/suggestions/{id}/review` | `{ "decision": "APPROVED" \| "REJECTED" }` ; 409 si déjà relue |
| GET | `/api/stats` | indicateurs |

## Tests et qualité

```bash
cd backend  && mvn verify           # tests + règle JaCoCo (échoue sous les seuils du pom.xml)
cd frontend && npm run typecheck && npm run test:coverage
```

- Backend : JUnit 5, AssertJ, Mockito, MockMvc, test d'intégration `@SpringBootTest` du parcours complet.
- Frontend : Vitest, Testing Library, faux serveur `fetch` en mémoire.
- Seuils de couverture (modifiables) : backend lignes 80 % / branches 70 % dans `pom.xml` ; frontend dans `vite.config.ts`. À relever une fois les valeurs réelles mesurées sur votre machine.

## Pipeline GitLab CI (`.gitlab-ci.yml`)

| Étape | Job | Rôle |
|---|---|---|
| test | `backend:test` | `mvn verify`, rapport JaCoCo, rapport JUnit, couverture affichée dans GitLab |
| test | `frontend:test` | typage, tests, seuil de couverture |
| quality | `sonarqube` | optionnel, actif seulement si `SONAR_HOST_URL` et `SONAR_TOKEN` sont définis |
| package | `package:*` | images Docker, branche par défaut uniquement |

## Guide de bonnes pratiques IA

Voir [`docs/AI_GUIDELINES.md`](docs/AI_GUIDELINES.md) : données interdites, validation humaine, qualité, licences, indicateurs, adoption, checklist de merge request.

## Limites assumées

- Le parseur est à base d'expressions régulières : pas de support des annotations avec parenthèses dans les paramètres, ni des classes imbriquées.
- Les squelettes générés contiennent des `TODO` : l'assertion attendue reste une décision humaine.
- Le masquage par motifs attrape les cas courants, pas tous les cas.
- Pas d'authentification ni de persistance : démonstrateur, pas un produit.

## Pistes d'évolution

- Implémentation `LlmTestGenerator` (appel d'un modèle derrière l'interface `TestGenerator`).
- Persistance (PostgreSQL ou MongoDB), authentification, historique par utilisateur.
- Test de mutation (PIT) dans la CI.
- Support de Kotlin ou C# (.NET) via de nouveaux parseurs.
