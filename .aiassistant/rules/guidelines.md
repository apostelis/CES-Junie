---
apply: always
---

Project Guidelines (Updated: 2025-08-12)

Stack and versions:
- Java 21+
- Spring Boot 3.x (latest compatible)
- JUnit 5
- React + Next.js 15 (Node.js 20 LTS+)
- Maven 3.9+

Workflow:
- Use Git with feature branches.
- Commit only after all tests pass locally.
- Prefer conventional commits (feat, fix, docs, test, refactor, chore).

Architecture:
- Follow DDD and Hexagonal (Ports & Adapters).
- Keep domain and application layers free of framework dependencies.

Testing:
- Create tests for new features.
- Always make sure tests pass locally.
- Run tests automatically in CI for every push/PR.
- Backend: mvn -q -DskipITs test; verify with mvn clean verify for coverage.
- Frontend: cd frontend && npm install && npm test.

Documentation:
- Update relevant documentation under the docs/ folder when behavior, APIs, or configs change.

Database:
- When the model changes, add Liquibase changeSets (with clear ids and rollbacks) to migrate the schema.