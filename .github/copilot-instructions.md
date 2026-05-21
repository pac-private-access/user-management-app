Build, test, and lint commands

- Build (full): mvn clean package
- Run locally: mvn spring-boot:run  (app starts on server.port=8081 by default)
- Run packaged jar: java -jar target/demo-0.0.1-SNAPSHOT.jar
- Tests (full suite): mvn test
- Run a single test class: mvn -Dtest=DemoApplicationTests test
- Run a single test method: mvn -Dtest=DemoApplicationTests#contextLoads test
- Linting: No dedicated linter configured. Add Checkstyle/SpotBugs/PMD via Maven if desired.

High-level architecture

- Spring Boot (Java 17) web application using Spring MVC + Thymeleaf for server-side rendered pages.
- Persistence: Spring Data JPA. Development runtime DB is H2 (jdbc:h2:mem:testdb) configured in application.properties; project includes Postgres and SQLite drivers. Production target: Supabase (Postgres).
- Typical package layout:
  - com.pac.UserManagementApp.java — application entrypoint
  - PAC.model — JPA entity classes (Employee, AppUser, BaseEmployee, DTOs)
  - PAC.repository — Spring Data repositories
  - PAC.service — business/service layer (EmployeeService exists)
  - PAC.controller — server-side controllers returning Thymeleaf views (UserWebController); add API controllers under PAC.api or PAC.controller.api for REST endpoints
  - src/main/resources/templates — Thymeleaf templates (login.html, users.html)
- Runtime behavior: application.properties sets spring.jpa.hibernate.ddl-auto=create so the schema is re-created on start; H2 console enabled at /h2-console.

Access-management design (project-specific)

- Two role types: ADMIN (system administrators) and REGULAR (parking entry guard). Role-based behaviour must be enforced both at the backend (endpoints/services) and in the server-rendered views (templates).
- Data source: Supabase (Postgres). The app should connect to Supabase for real deployments. Prefer using Spring profiles and environment variables to supply SUPABASE_URL and SUPABASE_DB_URL / JDBC connection string and credentials. Do NOT commit credentials into source control.
- Endpoints:
  - CRUD API endpoints: implement full create/read/update/delete for entities in PAC.model via @RestController endpoints (suggest path prefix /api/*). These are the endpoints used by integrations and admin tools.
  - Web/front-end endpoints: Thymeleaf-based controllers that render pages and adapt content by user role (ADMIN vs REGULAR). Keep web controllers (returning views) separate from REST controllers.
- Authentication & authorization:
  - Integrate Spring Security for sign-in. Use BCrypt password hashing and store hashed passwords in the DB. Replace current plaintext checks ("parola") in the prototype controller.
  - Recommended: role-based access control with GrantedAuthority (roles "ROLE_ADMIN", "ROLE_REGULAR"). Protect /api/** endpoints and template routes accordingly.
  - Session vs token: server-side sessions with Spring Security are acceptable for this server-rendered app. If API consumers need stateless auth, add JWT tokens or API keys for /api/**.
- Templates/UI behavior:
  - Templates should check the authenticated user's authorities and render different menus/actions for ADMIN vs REGULAR. Keep responsibility for policy enforcement on the server (controllers/services) not just in templates.

Key conventions and repo-specific notes

- Mixed package roots: both com.pac and PAC packages exist. When tracing a feature, search both roots.
- Controller conventions:
  - Use PAC.controller for web controllers (Thymeleaf) and PAC.api (or PAC.controller.api) for REST controllers. Keep mapping prefixes consistent (/users, /login for web; /api/users for REST).
- Repository & DTO usage:
  - Repositories under PAC.repository should remain thin; place business logic in services (PAC.service). Use DTOs (PAC.dto) for API payloads where appropriate.
- Database migrations:
  - Do not rely on spring.jpa.hibernate.ddl-auto=create for production. Add Flyway/Liquibase migrations and keep schema evolution in version control.
- Supabase specifics:
  - Treat Supabase as Postgres. Use the Postgres JDBC URL from the Supabase project settings. Consider using Supabase roles and Row-Level Security in the DB, but enforce authorization in the app.
- Security notes in code:
  - Current login flow compares plaintext password (param name "parola"). Replace with Spring Security authentication manager and password encoder. Tests should include role-based access checks.

Files to check early when implementing or troubleshooting

- pom.xml — dependency & Java version (17); add spring-boot-starter-security and Flyway here when implementing auth/migrations
- application.properties / application-{profile}.properties — add Supabase JDBC URL, credentials via env vars, active profile
- src/main/resources/templates/* — Thymeleaf templates; update to conditionally render by authority
- PAC.repository.* and PAC.model.* — JPA mapping & queries; align with Supabase schema
- PAC.service.* — place for business rules and access checks
- src/test/java — tests; add integration tests that use a test profile pointing to H2

Existing AI/assistant configs

- No CLAUDE.md, AGENTS.md, .cursorrules, .windsurfrules or other AI assistant rule files detected. Add them here if an external assistant requires special instructions.

Recommended immediate changes for this project

- Add spring-boot-starter-security and configure WebSecurityConfigurerAdapter / SecurityFilterChain to support form login and role mappings.
- Replace plaintext login logic with Spring Security + BCrypt and seed an ADMIN user via data.sql or a Flyway migration for first-run.
- Add Flyway and convert schema creation from ddl-auto to migrations.
- Add environment-variable-driven profiles for Supabase credentials (e.g., SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD).
- Add integration tests for role-based access and for REST API CRUD behavior.

