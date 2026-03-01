# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FIS Portal — Angular 17 frontend + Spring Boot 2.7.18 microservices backend with custom JWT auth (not Keycloak despite the project name). Single PostgreSQL instance with multiple schemas, Redis for caching, and an HTTP gateway proxy.

---

## Common Commands

### Angular (in `my-angular-app/`)
```bash
npm start          # Dev server on :4200, proxies /api/* → gateway :8081
npm run build      # Production build to dist/my-angular-app
npm run test       # Karma + Jasmine unit tests
npm run watch      # Build in watch mode
```

### Java / Gradle (in `spring-boot-keycloak/`)
```bash
# Build a specific service (REQUIRED before Docker build)
./gradlew :auth-service:build -x test
./gradlew :gateway-service:build -x test
# ... same pattern for: user-service, ticket-service, workflow-service, audit-service, report-service

# Build all services
./gradlew build -x test

# Run tests for a specific service
./gradlew :auth-service:test
```

### Docker
**CRITICAL**: Dockerfiles copy pre-built JARs from `build/libs/` — Docker does NOT compile Java. Always Gradle-build before Docker-build.

```bash
# Use `docker compose` (v2), NOT `docker-compose` (v1 fails with KeyError: 'ContainerConfig')
docker compose up -d

# Full rebuild sequence for a single service:
./gradlew :auth-service:build -x test
docker compose build auth-service
docker compose up -d auth-service
```

Services take 115–165s to start. Health checks use `/actuator/health` with `start_period: 180s`.

---

## Architecture

### Microservices & Ports
| Service | Port | Description |
|---|---|---|
| `gateway-service` | 8081 | HTTP reverse proxy — all frontend traffic goes here |
| `auth-service` | 8082 | Login, JWT generation, roles & permissions |
| `user-service` | 8083 | User profiles, departments, branches |
| `ticket-service` | 8084 | Tickets, comments, attachments |
| `workflow-service` | 8085 | Approval workflows |
| `audit-service` | 8086 | Audit logs (JSONB metadata) |
| `report-service` | 8087 | JasperReports PDF generation |

Gateway routing (see `ProxyController.java`):
```
/api/auth/**     → auth-service:8082
/api/roles/**    → auth-service:8082
/api/users/**    → user-service:8083
/api/tickets/**  → ticket-service:8084
/api/workflow/** → workflow-service:8085
/api/audit/**    → audit-service:8086
/api/reports/**  → report-service:8087
```

### Auth Flow
1. `POST /api/auth/login` → `auth-service` validates credentials (bcrypt), returns JWT (HS256)
2. Frontend stores JWT, sends `Authorization: Bearer {token}` on all requests
3. `JwtAuthenticationFilter` (shared-lib) validates token and sets Spring `SecurityContext`
4. `SecurityUtils.getCurrentUser()` / `.getCurrentUserId()` retrieves current user in service layer

### Database
Single PostgreSQL on port 5433 with separate schemas:
- `auth` — users, roles, permissions, user_role, refresh_tokens
- `usr` — user_profile, department, branch, user_organization
- `ticket` — ticket, category, priority, status_history, comment, attachment
- `workflow` — approval_request, approval_step, approval_history
- `audit` — audit_log

Schema managed manually via `init-db/01-init.sql`. Hibernate `ddl-auto: none`.

### Shared Library (`shared-lib`)
All microservices depend on this module. Contains:
- `JwtValidator` — validates JWT, extracts claims (username, roles, permissions, userId, email)
- `JwtAuthenticationFilter` — `OncePerRequestFilter`, sets `SecurityContext`
- `SecurityUtils` — thread-local user context (`getCurrentUser()`, `getCurrentUserId()`)

---

## Java Patterns

### New Microservice Checklist
1. Add `@SpringBootApplication(scanBasePackages = "com.example")` on main class — required to scan `JwtAuthenticationFilter` from shared-lib
2. Add `jwt: secret: ${JWT_SECRET}` in `application.yml`
3. Extend `WebSecurityConfigurerAdapter` and inject + add `JwtAuthenticationFilter` to filter chain
4. Add `spring-boot-starter-actuator` to `build.gradle` dependencies
5. Add gateway route in `ProxyController.java`
6. Add Docker service in `docker-compose.yml` with health check

### Entity Pattern
```java
@Id
@GeneratedValue(generator = "UUID")
@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
@Column(name = "id", updatable = false, nullable = false)
private UUID id;
```

### JSONB Fields
```java
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
// on the entity class, then on the field:
@Type(type = "jsonb")
@Column(columnDefinition = "jsonb")
private Map<String, Object> metadata;
```

### Security Config (Spring Boot 2.7)
```java
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired private JwtAuthenticationFilter jwtAuthenticationFilter;
    // addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
}
```

---

## Angular Patterns

### Standalone Components
All components are standalone. Always include required modules in `imports: [CommonModule, FormsModule, ...]`.

### Permission System
Permissions are stored as codes in the JWT. Available codes: `TICKET_VIEW`, `TICKET_MANAGE`, `USER_VIEW`, `USER_MANAGE`, `EMAIL_TEMPLATE_VIEW`, `EMAIL_TEMPLATE_MANAGE`, `ROLE_VIEW`, `ROLE_MANAGE`, `AUDIT_VIEW`, `WORKFLOW_VIEW`, `WORKFLOW_MANAGE`.

Route guard:
```typescript
canActivate: [permissionGuard], data: { permission: 'USER_MANAGE' }
```

Template directive:
```html
<button *appPermission="'USER_MANAGE'">Edit</button>
```

### Pagination
All paginated APIs return `Page<T> { content, totalElements, totalPages, size, number }` (Spring Data format, 0-indexed page number).

### Services
Angular services live in `my-angular-app/src/app/services/`. Key services: `AuthService`, `RolePermissionService`, `ToastService`, `AuditService`, `WorkflowService`.

Toast notifications: `this.toastService.success('msg')` / `.error('msg')`.

---

## Key File Locations

| Purpose | Path |
|---|---|
| Angular routes | `my-angular-app/src/app/app.routes.ts` |
| Angular proxy config | `my-angular-app/proxy.conf.json` |
| DB schema init | `init-db/01-init.sql` |
| Gradle multi-module build | `spring-boot-keycloak/build.gradle` |
| Docker compose | `docker-compose.yml` |
| Gateway proxy controller | `spring-boot-keycloak/gateway-service/src/main/java/com/example/gateway/controller/ProxyController.java` |
| Shared lib (JWT, security) | `spring-boot-keycloak/shared-lib/src/main/java/com/example/` |
| Admin layout component | `my-angular-app/src/app/admin/admin-layout.component.{ts,html,css}` |
