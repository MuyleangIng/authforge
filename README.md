## 🎯 What You Are Building

**AuthForge** — An open-source Identity & Access Management (IAM) platform.
Think Keycloak, but modern: Spring Boot 3, Redis, Kafka, PostgreSQL, with first-class SDKs for React, Next.js, Python, and Laravel.

### Core Features to Implement
- OAuth2 / OpenID Connect (authorization code, password, refresh, client credentials)
- Multi-realm / multi-tenant support
- JWT access tokens + refresh token rotation
- SSO (Single Sign-On) with hosted login UI
- Role-Based Access Control (RBAC)
- Redis token cache (sub-ms validation)
- Kafka event bus (real-time auth events)
- Admin REST API + GraphQL
- Built-in analytics dashboard
- WebSocket real-time session monitoring
- SDK integrations: React, Next.js, Python, Laravel

---

## 📁 Required Project Structure

You MUST generate the project with this exact top-level structure:

```
authforge/
├── build/                        ← Gradle build output (generated, do not create)
├── build.gradle                  ← Root Gradle build file
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew                       ← Unix Gradle wrapper script
├── gradlew.bat                   ← Windows Gradle wrapper script
├── settings.gradle               ← Project name + submodule declarations
└── src/
    ├── main/
    │   ├── java/
    │   │   └── io/authforge/
    │   │       ├── AuthForgeApplication.java
    │   │       ├── config/
    │   │       ├── controller/
    │   │       ├── domain/
    │   │       ├── dto/
    │   │       ├── event/
    │   │       ├── exception/
    │   │       ├── repository/
    │   │       ├── security/
    │   │       └── service/
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       ├── application-prod.yml
    │       ├── db/migration/       ← Flyway SQL migrations
    │       │   ├── V1__init_schema.sql
    │       │   ├── V2__seed_master_realm.sql
    │       │   └── V3__seed_default_roles.sql
    │       ├── static/             ← Frontend assets (login UI, admin UI)
    │       │   ├── css/
    │       │   ├── js/
    │       │   └── img/
    │       └── templates/          ← Thymeleaf templates
    │           ├── login.html
    │           ├── register.html
    │           └── error.html
    └── test/
        └── java/
            └── io/authforge/
                ├── AuthForgeApplicationTests.java
                ├── controller/
                └── service/
```

---

## ⚙️ Gradle Configuration

### `settings.gradle`
```groovy
rootProject.name = 'authforge'
```

### `build.gradle`
```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.3.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'io.authforge'
version = '1.0.0'
sourceCompatibility = '21'

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Core
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-websocket'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    implementation 'org.springframework.boot:spring-boot-starter-mail'
    implementation 'org.springframework.boot:spring-boot-starter-cache'

    // OAuth2 / JWT
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
    implementation 'org.springframework.security:spring-security-oauth2-authorization-server:1.2.3'
    implementation 'io.jsonwebtoken:jjwt-api:0.12.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.5'

    // Kafka
    implementation 'org.springframework.kafka:spring-kafka'

    // Database
    runtimeOnly 'org.postgresql:postgresql'
    implementation 'org.flywaydb:flyway-core'
    implementation 'org.flywaydb:flyway-database-postgresql'

    // GraphQL
    implementation 'org.springframework.boot:spring-boot-starter-graphql'

    // Redis
    implementation 'org.springframework.session:spring-session-data-redis'

    // Utilities
    implementation 'org.mapstruct:mapstruct:1.5.5.Final'
    annotationProcessor 'org.mapstruct:mapstruct-processor:1.5.5.Final'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.4.0'
    implementation 'com.github.ua-parser:uap-java:1.6.1'

    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'org.springframework.kafka:spring-kafka-test'
    testImplementation 'com.h2database:h2'
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:postgresql'
    testImplementation 'org.testcontainers:kafka'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

### `gradle/wrapper/gradle-wrapper.properties`
```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

---

## 🗄️ Database Schema (Flyway Migrations)

### `V1__init_schema.sql`
```sql
-- REALMS
CREATE TABLE realms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) UNIQUE NOT NULL,
    display_name VARCHAR(200),
    enabled BOOLEAN DEFAULT TRUE,
    registration_allowed BOOLEAN DEFAULT TRUE,
    access_token_lifespan INT DEFAULT 300,
    refresh_token_lifespan INT DEFAULT 1800,
    sso_session_idle INT DEFAULT 1800,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- USERS
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    realm_id UUID NOT NULL REFERENCES realms(id) ON DELETE CASCADE,
    username VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    enabled BOOLEAN DEFAULT TRUE,
    email_verified BOOLEAN DEFAULT FALSE,
    last_login TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(realm_id, username),
    UNIQUE(realm_id, email)
);

-- ROLES
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    realm_id UUID NOT NULL REFERENCES realms(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_composite BOOLEAN DEFAULT FALSE,
    client_role BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(realm_id, name)
);

-- USER ROLES
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- OAUTH2 CLIENTS
CREATE TABLE clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    realm_id UUID NOT NULL REFERENCES realms(id) ON DELETE CASCADE,
    client_id VARCHAR(100) NOT NULL,
    client_secret VARCHAR(255),
    name VARCHAR(200),
    description TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    public_client BOOLEAN DEFAULT FALSE,
    redirect_uris TEXT DEFAULT '[]',
    web_origins TEXT DEFAULT '[]',
    grant_types TEXT DEFAULT '["authorization_code","refresh_token"]',
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(realm_id, client_id)
);

-- SESSIONS (SSO)
CREATE TABLE sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    realm_id UUID NOT NULL REFERENCES realms(id) ON DELETE CASCADE,
    client_id VARCHAR(100),
    ip_address VARCHAR(45),
    user_agent TEXT,
    refresh_token TEXT UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    last_accessed TIMESTAMP DEFAULT NOW()
);

-- AUTHORIZATION CODES
CREATE TABLE auth_codes (
    code VARCHAR(255) PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL,
    user_id UUID NOT NULL,
    realm_id UUID NOT NULL,
    redirect_uri TEXT,
    scope TEXT,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- AUDIT LOG
CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    realm_id UUID,
    user_id UUID,
    event_type VARCHAR(100) NOT NULL,
    client_id VARCHAR(100),
    ip_address VARCHAR(45),
    details JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_users_realm ON users(realm_id);
CREATE INDEX idx_sessions_user ON sessions(user_id);
CREATE INDEX idx_sessions_refresh ON sessions(refresh_token);
CREATE INDEX idx_audit_realm ON audit_log(realm_id);
CREATE INDEX idx_audit_user ON audit_log(user_id);
CREATE INDEX idx_audit_event ON audit_log(event_type);
```

---

## 🏗️ Application Configuration

### `application.yml`
```yaml
spring:
  application:
    name: authforge

  datasource:
    url: jdbc:postgresql://localhost:5432/authforge
    username: authforge
    password: authforge_secret
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

  data:
    redis:
      host: localhost
      port: 6379
      password: ""
      timeout: 2000ms

  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: authforge-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer

  session:
    store-type: redis
    timeout: 30m

  graphql:
    graphiql:
      enabled: true
      path: /graphiql

  cache:
    type: redis

authforge:
  jwt:
    secret: "CHANGE_ME_IN_PRODUCTION_USE_256_BIT_KEY"
    issuer: "http://localhost:8080"
  admin:
    default-username: admin
    default-password: admin
    default-email: admin@localhost
  token:
    cache-ttl: 300
  cors:
    allowed-origins: "http://localhost:3000,http://localhost:5173"

server:
  port: 8080
  servlet:
    context-path: /

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
```

---

## 📦 Java Package Structure & Classes to Create

### Package: `io.authforge`

#### `AuthForgeApplication.java`
```java
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class AuthForgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthForgeApplication.class, args);
    }
}
```

---

### Package: `io.authforge.domain` (JPA Entities)

Create these entities with full Lombok + JPA annotations:

| Entity | Table | Key Fields |
|--------|-------|-----------|
| `Realm` | `realms` | id, name, displayName, enabled, accessTokenLifespan |
| `User` | `users` | id, realmId, username, email, passwordHash, enabled, emailVerified |
| `Role` | `roles` | id, realmId, name, description, isComposite |
| `Client` | `clients` | id, realmId, clientId, clientSecret, publicClient, redirectUris |
| `Session` | `sessions` | id, userId, realmId, refreshToken, expiresAt |
| `AuthCode` | `auth_codes` | code, clientId, userId, realmId, expiresAt, used |
| `AuditLog` | `audit_log` | id, realmId, userId, eventType, ipAddress, details |

---

### Package: `io.authforge.controller`

#### REST Controllers to implement:

**`AuthController`** — handles all OAuth2/OIDC endpoints
```
GET  /realms/{realm}/.well-known/openid-configuration
GET  /realms/{realm}/protocol/openid-connect/certs
POST /realms/{realm}/protocol/openid-connect/token
GET  /realms/{realm}/protocol/openid-connect/auth
POST /realms/{realm}/protocol/openid-connect/logout
GET  /realms/{realm}/protocol/openid-connect/userinfo
POST /realms/{realm}/protocol/openid-connect/token/introspect
POST /realms/{realm}/register
```

**`AdminRealmController`** — realm CRUD
```
GET    /admin/realms
POST   /admin/realms
GET    /admin/realms/{realm}
PUT    /admin/realms/{realm}
DELETE /admin/realms/{realm}
```

**`AdminUserController`** — user management
```
GET    /admin/realms/{realm}/users
POST   /admin/realms/{realm}/users
GET    /admin/realms/{realm}/users/{id}
PUT    /admin/realms/{realm}/users/{id}
DELETE /admin/realms/{realm}/users/{id}
GET    /admin/realms/{realm}/users/{id}/roles
POST   /admin/realms/{realm}/users/{id}/roles
DELETE /admin/realms/{realm}/users/{id}/roles/{roleId}
GET    /admin/realms/{realm}/users/{id}/sessions
DELETE /admin/realms/{realm}/users/{id}/sessions
```

**`AdminRoleController`** — role management
```
GET    /admin/realms/{realm}/roles
POST   /admin/realms/{realm}/roles
GET    /admin/realms/{realm}/roles/{id}
PUT    /admin/realms/{realm}/roles/{id}
DELETE /admin/realms/{realm}/roles/{id}
```

**`AdminClientController`** — OAuth2 client management
```
GET    /admin/realms/{realm}/clients
POST   /admin/realms/{realm}/clients
GET    /admin/realms/{realm}/clients/{id}
PUT    /admin/realms/{realm}/clients/{id}
DELETE /admin/realms/{realm}/clients/{id}
```

**`AdminSessionController`** — session monitoring
```
GET    /admin/realms/{realm}/sessions
DELETE /admin/realms/{realm}/sessions/{id}
```

**`AdminAuditController`** — audit log
```
GET    /admin/realms/{realm}/audit-log
```

**`AnalyticsController`** — metrics
```
GET    /admin/realms/{realm}/analytics/overview
GET    /admin/realms/{realm}/analytics/logins
GET    /admin/realms/{realm}/analytics/active-sessions
```

---

### Package: `io.authforge.service`

| Service | Responsibility |
|---------|---------------|
| `TokenService` | Generate + validate JWT. Redis cache for token validation. |
| `AuthService` | Login, register, logout. OAuth2 flows (password, auth_code, refresh, client_credentials). |
| `UserService` | CRUD, role assignment, password hashing (BCrypt strength 12). |
| `RealmService` | Realm CRUD, settings management. |
| `RoleService` | Role CRUD, assignment. |
| `ClientService` | OAuth2 client registration, secret generation. |
| `SessionService` | Create/revoke/expire sessions. Redis + DB. |
| `AuditService` | Write audit events. Publishes to Kafka. |
| `AnalyticsService` | Query audit log for metrics. |

---

### Package: `io.authforge.security`

| Class | Purpose |
|-------|---------|
| `SecurityConfig` | Spring Security filter chain. Permit /realms/**/token, /realms/**/auth, protect /admin/** |
| `JwtTokenFilter` | OncePerRequestFilter — extract + validate JWT from Authorization header |
| `JwtTokenProvider` | Build / parse / validate JWT using jjwt 0.12+ |
| `RedisTokenCache` | Cache token validation results in Redis with TTL |
| `CorsConfig` | CORS configuration from application.yml |

---

### Package: `io.authforge.event`

#### Kafka Topics to publish:
```
authforge.auth.login          ← on successful login
authforge.auth.login_failed   ← on failed login
authforge.auth.logout         ← on logout
authforge.auth.register       ← on registration
authforge.token.refresh       ← on token refresh
authforge.session.expired     ← on session expiry
authforge.admin.user.created  ← on user creation
authforge.admin.user.updated  ← on user update
authforge.admin.user.deleted  ← on user delete
```

#### `AuthEventProducer.java`
```java
@Component
public class AuthEventProducer {
    // Use KafkaTemplate<String, AuthEvent>
    // Publish events to all topics listed above
    // Key = realmName:userId
}
```

#### `AuthEvent.java` (record)
```java
public record AuthEvent(
    String eventType,
    String realmId,
    String userId,
    String clientId,
    String ipAddress,
    Instant timestamp,
    Map<String, Object> details
) {}
```

---

### Package: `io.authforge.config`

| Config Class | Purpose |
|-------------|---------|
| `RedisConfig` | RedisTemplate, ObjectMapper |
| `KafkaConfig` | KafkaAdmin, topic creation |
| `CacheConfig` | CacheManager with Redis |
| `WebSocketConfig` | STOMP over WebSocket for real-time sessions |
| `GraphQLConfig` | Schema + resolvers |
| `OpenApiConfig` | Swagger/OpenAPI 3.1 docs |
| `SchedulerConfig` | Cleanup expired sessions every 5 min |

---

## 🌐 Frontend (Hosted in Spring Boot)

Serve these HTML pages via Thymeleaf at:

### `templates/login.html`
- Full login form with realm name displayed
- OAuth2 authorization code flow
- Links to register
- Styled with embedded CSS (dark theme, professional)
- Form POSTs to `/realms/{realm}/protocol/openid-connect/token`
- JS redirects with auth code after login

### `templates/register.html`
- Registration form: username, email, password, first/last name
- POSTs to `/realms/{realm}/register`
- Redirects to login on success

### `templates/error.html`
- Generic error page for 4xx/5xx

---

## 🔌 SDK Integration Code (Put in `/docs/sdks/`)

### React SDK (`/docs/sdks/react.md`)
```jsx
// Install
npm install @authforge/react

// AuthForgeProvider wraps your app
import { AuthForgeProvider, useAuth } from '@authforge/react'

function App() {
  return (
    <AuthForgeProvider
      url="http://localhost:8080"
      realm="myrealm"
      clientId="my-react-app"
    >
      <Router />
    </AuthForgeProvider>
  )
}

// useAuth hook
const { user, token, login, logout, hasRole, isAuthenticated } = useAuth()
```

### Next.js SDK (`/docs/sdks/nextjs.md`)
```ts
// middleware.ts
import { withAuth } from '@authforge/nextjs'
export default withAuth({ realm: 'myrealm', clientId: 'my-next-app' })
export const config = { matcher: ['/dashboard/:path*'] }

// Server component
import { getServerSession } from '@authforge/nextjs/server'
const session = await getServerSession()

// Client component
'use client'
import { useAuth } from '@authforge/nextjs/client'
const { user, login, logout } = useAuth()
```

### Python SDK (`/docs/sdks/python.md`)
```python
# pip install authforge-python

from authforge import AuthForgeClient
from authforge.fastapi import require_auth, require_role, get_current_user

client = AuthForgeClient(
    url="http://localhost:8080",
    realm="myrealm",
    client_id="my-python-api",
    client_secret="secret"
)

@app.get("/me")
@require_auth(client)
async def me(user = Depends(get_current_user(client))):
    return user

@app.delete("/users/{id}")
@require_role(client, "admin")
async def delete(id: str):
    ...
```

### Laravel SDK (`/docs/sdks/laravel.md`)
```php
// composer require authforge/laravel
// config/authforge.php

Route::middleware('authforge')->group(function () {
    Route::get('/profile', [ProfileController::class, 'show']);
});

Route::middleware('authforge:admin')->group(function () {
    Route::apiResource('users', AdminUserController::class);
});

// In controller
$user = auth()->user(); // AuthForge user object
$user->hasRole('admin');
$user->getRoles();
```

---

## 🐳 Docker Compose (`docker-compose.yml`)

```yaml
version: '3.9'
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: authforge
      POSTGRES_USER: authforge
      POSTGRES_PASSWORD: authforge_secret
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data

  zookeeper:
    image: confluentinc/cp-zookeeper:7.6.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.6.0
    depends_on: [zookeeper]
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"

  authforge:
    build: .
    ports:
      - "8080:8080"
    depends_on: [postgres, redis, kafka]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/authforge
      SPRING_DATA_REDIS_HOST: redis
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092

volumes:
  postgres_data:
  redis_data:
```

---

## 📋 Build & Run Instructions

```bash
# 1. Start infrastructure
docker-compose up -d postgres redis zookeeper kafka

# 2. Build
./gradlew clean build

# 3. Run
./gradlew bootRun

# OR run the JAR
java -jar build/libs/authforge-1.0.0.jar

# 4. Access
# API:     http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui.html
# GraphQL: http://localhost:8080/graphiql

# 5. Default credentials
# Username: admin
# Password: admin
# Realm:    master
```

---

## ✅ Implementation Checklist

Follow this order when building:

### Phase 1 — Foundation
- [ ] Create Gradle wrapper files (gradlew, gradlew.bat, gradle/wrapper/)
- [ ] Write `build.gradle` and `settings.gradle`
- [ ] Write `application.yml` (dev + prod profiles)
- [ ] Create all JPA entities in `io.authforge.domain`
- [ ] Write all Flyway migration SQL files
- [ ] Seed master realm + admin user on startup (`ApplicationRunner`)

### Phase 2 — Core Auth
- [ ] `TokenService` — JWT generation + Redis cache
- [ ] `AuthService` — all grant types
- [ ] `AuthController` — all OIDC endpoints
- [ ] `SecurityConfig` — protect routes, JWT filter
- [ ] `SessionService` — create/revoke sessions

### Phase 3 — Admin API
- [ ] All Admin controllers (realm, user, role, client, session, audit)
- [ ] All corresponding Services
- [ ] Request validation (Jakarta Bean Validation)
- [ ] Global exception handler (`@RestControllerAdvice`)

### Phase 4 — Events & Cache
- [ ] `KafkaConfig` + topic creation
- [ ] `AuthEventProducer` — publish all auth events
- [ ] `AuditService` — write to DB + Kafka
- [ ] `RedisTokenCache` — cache token validation results
- [ ] Session expiry scheduler (every 5 min)

### Phase 5 — Frontend
- [ ] `login.html` — Thymeleaf SSO login page
- [ ] `register.html` — self-service registration
- [ ] `error.html` — error page

### Phase 6 — Extras
- [ ] GraphQL schema + resolvers (admin queries)
- [ ] WebSocket session monitor endpoint
- [ ] Analytics endpoints
- [ ] Swagger/OpenAPI docs
- [ ] Docker + docker-compose.yml
- [ ] `/docs/sdks/` integration guides

---

## 🚫 Rules & Constraints

1. **Use Spring Boot 3.3.x** — Do NOT use Spring Boot 2.x
2. **Java 21** — use records, pattern matching, sealed classes where appropriate
3. **Use Lombok** — `@Data`, `@Builder`, `@Slf4j`, `@RequiredArgsConstructor`
4. **BCrypt cost 12** for all password hashing
5. **All admin endpoints require `admin` role** — enforce with `@PreAuthorize("hasRole('admin')")`
6. **All tokens validated via Redis** before hitting the DB
7. **Every auth event published to Kafka** before returning response
8. **Flyway only** for DB migrations — never use `ddl-auto: create`
9. **No hardcoded secrets** — use `application.yml` or env vars
10. **Write tests** for all service classes using JUnit 5 + Mockito

---

## 🔑 Default Seed Data

On first startup, auto-create:

| Item | Value |
|------|-------|
| Realm | `master` |
| Admin user | `admin` / `admin` |
| Admin email | `admin@localhost` |
| Roles | `admin`, `user` |
| Default client | `admin-cli` (confidential) |
| Default client | `account` (public, for React/Next.js) |

---

## 📖 API Quick Reference

```
# Get OIDC discovery
GET /realms/master/.well-known/openid-configuration

# Login (password grant)
POST /realms/master/protocol/openid-connect/token
Body: grant_type=password&username=admin&password=admin&client_id=admin-cli&client_secret=xxx

# Register user
POST /realms/master/register
Body: { "username": "john", "email": "john@example.com", "password": "secret123" }

# Get all users (admin)
GET /admin/realms/master/users
Header: Authorization: Bearer <admin_access_token>

# Create realm
POST /admin/realms
Body: { "name": "myapp", "displayName": "My Application" }
```

