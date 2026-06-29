# Repository Guidelines

## Project Structure & Module Organization
`Core/` contains the Spring Boot backend, with Java sources under `src/main/java`, Oracle SQL scripts under `src/main/resources/db`, and tests under `src/test/java`. `Front/` contains the Vue 3 + TypeScript frontend; main views live in `src/views`, API clients in `src/api`, and shared types in `src/types`. `build/` holds Windows build/start scripts, and `docs/` stores requirements, permissions, and agent prompts.

## Build, Test, and Development Commands
- `cd Front && npm run dev`: start the Vite frontend locally.
- `cd Front && npm run build`: type-check and build the frontend production bundle.
- `cd Core && mvn -q -DskipTests compile`: compile the backend quickly.
- `cd Core && mvn test`: run backend tests.
- `.\build\build.bat`: build frontend, package the Spring Boot jar, and copy SQL scripts into `build/sql`.
- `.\build\start.bat` or `.\build\start.bat -b`: start the demo stack in foreground or background mode.
- `docker compose up -d`: start the Oracle 21c container when using Docker.

## Coding Style & Naming Conventions
Use 4 spaces in Java and 2 spaces in Vue/TypeScript. Keep Java packages under `pers.luoluo.databasekeshe.<domain>` and follow the existing `controller/service/mapper/dto` split. Use `PascalCase` for Java classes, `camelCase` for methods and fields, and `UPPER_SNAKE_CASE` for SQL constants or Oracle object names. Prefer thin Java services when logic can live in Oracle packages or SQL scripts.

## Testing Guidelines
Backend tests use JUnit 5 through Spring Boot. Add tests under `Core/src/test/java` with names ending in `Tests`. Run `mvn test` before submitting backend changes. The frontend currently has no dedicated test runner, so at minimum verify `npm run build` and manually check affected screens.

## Commit & Pull Request Guidelines
Recent history mixes short commits (`update`) with clearer Conventional Commit style (`feat: ...`). Prefer descriptive messages such as `feat(query): archive cross-day history results` or `fix(front): stabilize history chart rendering`. Keep pull requests focused and include:
- a short summary of behavior changes,
- affected modules (`Core`, `Front`, SQL),
- setup or migration notes for Oracle scripts,
- screenshots for UI changes.

## Security & Configuration Tips
Do not commit real secrets in `.env`. Use `.env.template` as the baseline. Database schema updates should be applied through `Core/src/main/resources/db/oracle21c-init.sql` and related scripts so local Docker, local Oracle, and release builds stay aligned.
