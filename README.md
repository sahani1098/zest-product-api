# Zest India — Product API

A production-ready RESTful API for Product CRUD operations built with **Java 21**, **Spring Boot 3.4.2**, **JWT Authentication**, and **MySQL 8.0**.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.2 |
| ORM | Spring Data JPA (Hibernate) |
| Database | MySQL 8.0 |
| Security | Spring Security + JWT + Refresh Token |
| Testing | JUnit 5 + Mockito + H2 |
| Docs | Swagger / OpenAPI 3 |
| Container | Docker + Docker Compose |

---

## Architecture
```
zest-product-api/
├── controller/       # REST controllers (API layer)
├── service/          # Business logic
├── repository/       # Data access (Spring Data JPA)
├── entity/           # JPA entities (Product, Item, AppUser)
├── dto/              # Request/Response DTOs
├── security/         # JWT filter, UserDetailsService, JwtUtil
├── config/           # SecurityConfig, OpenApiConfig
└── exception/        # GlobalExceptionHandler, custom exceptions
```

### Key Design Decisions
- **Stateless JWT auth** — access token (15 min) + refresh token (7 days) with rotation
- **Role-based access** — `USER` can read/write products, `ADMIN` can also delete
- **Pagination** on all collection endpoints
- **Async logging** via `@Async` for non-blocking operations
- **Standardized responses** via `ApiResponse<T>` wrapper
- **Global exception handling** with structured error responses

---

## Quick Start — Docker (Recommended)

**Prerequisites:** Docker Desktop installed and running. No Java, Maven or MySQL needed locally.
```bash
# Step 1 — Build the JAR (needs internet, only once)
mvn clean package -DskipTests

# Step 2 — Start MySQL + App
docker compose up -d

# Step 3 — Check both containers are running
docker ps
```

You should see:
```
zest-product-api   Up (healthy)   0.0.0.0:8080->8080/tcp
zest-mysql         Up (healthy)   0.0.0.0:3307->3306/tcp
```

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **API Docs:** http://localhost:8080/api-docs

### Stop the app
```bash
docker compose down        # stop (keeps database data)
docker compose down -v     # stop + delete database (fresh start)
```

---

## Quick Start — Local Development (No Docker)

### Prerequisites
- Java 21+
- Maven 3.9+
- MySQL 8.0 running locally

### Step 1 — Create the database
```sql
CREATE DATABASE IF NOT EXISTS productdb;
CREATE USER IF NOT EXISTS 'zestuser'@'localhost' IDENTIFIED BY 'zestpass';
GRANT ALL PRIVILEGES ON productdb.* TO 'zestuser'@'localhost';
FLUSH PRIVILEGES;
```

### Step 2 — Run the app
```bash
mvn spring-boot:run
```

### Step 3 — Open Swagger
```
http://localhost:8080/swagger-ui.html
```

---

## Role-Based Access Control

| Action | USER | ADMIN |
|--------|------|-------|
| Register / Login | ✅ | ✅ |
| View products | ✅ | ✅ |
| Create product | ✅ | ✅ |
| Update product | ✅ | ✅ |
| Delete product | ❌ 403 Forbidden | ✅ |

> Every newly registered user gets **USER** role by default.
> To delete products, the user must be promoted to **ADMIN** role manually via database.

---

## Complete Workflow — Step by Step

### USER Workflow

#### 1. Register
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@example.com","password":"secret123"}'
```
Response:
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "accessToken": "eyJhbGci......",
    "refreshToken": "eyJhbGci......",
    "tokenType": "Bearer",
    "expiresIn": 900000
  }
}
```

#### 2. Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"secret123"}'
```
Copy the `accessToken` from the response and use it in all product requests.

#### 3. Create a Product
```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer <your_access_token>" \
  -H "Content-Type: application/json" \
  -d '{"productName":"Widget X"}'
```

#### 4. List Products
```bash
curl "http://localhost:8080/api/v1/products?page=0&size=10" \
  -H "Authorization: Bearer <your_access_token>"
```
> Pagination is zero-based — use page=0 for first page, page=1 for second page.

#### 5. Update a Product
```bash
curl -X PUT http://localhost:8080/api/v1/products/1 \
  -H "Authorization: Bearer <your_access_token>" \
  -H "Content-Type: application/json" \
  -d '{"productName":"Widget Y"}'
```

#### 6. Try Delete — USER gets 403
```bash
curl -X DELETE http://localhost:8080/api/v1/products/1 \
  -H "Authorization: Bearer <your_access_token>"
```
Response:
```json
{
  "success": false,
  "message": "Access denied: insufficient permissions"
}
```

#### 7. Refresh Token (when access token expires after 15 min)
```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<your_refresh_token>"}'
```

---

### How to Make a User ADMIN

By default every registered user has USER role only.
To promote a user to ADMIN follow these steps:

#### Step 1 — Open the database

Docker:
```bash
docker exec -it zest-mysql mysql -u zestuser -pzestpass productdb
```

Local:
```bash
mysql -u zestuser -pzestpass productdb
```

#### Step 2 — Check the user ID
```sql
SELECT id, username, email FROM app_user;
```

#### Step 3 — Check current roles
```sql
SELECT * FROM user_roles;
```

#### Step 4 — Grant ADMIN role
```sql
INSERT INTO user_roles (user_id, role) VALUES (1, 'ADMIN');
```
Replace 1 with the actual user ID from Step 2.

#### Step 5 — Verify the role was added
```sql
SELECT u.username, r.role
FROM app_user u
JOIN user_roles r ON u.id = r.user_id;
```

#### Step 6 — Exit MySQL
```sql
exit
```

#### Step 7 — Login again to get a fresh ADMIN token
The old token still has USER role. You MUST login again to get a new token with ADMIN role.
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"secret123"}'
```

---

### ADMIN Workflow

After logging in with ADMIN token you can now DELETE products:

#### Delete a Product (ADMIN only)
```bash
curl -X DELETE http://localhost:8080/api/v1/products/1 \
  -H "Authorization: Bearer <your_admin_access_token>"
```
Response:
```json
{
  "success": true,
  "message": "Product deleted successfully"
}
```

---

## API Endpoints

### Auth (no token required)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register new user (gets USER role) |
| POST | `/api/v1/auth/login` | Login, get JWT tokens |
| POST | `/api/v1/auth/refresh` | Refresh access token |

### Products (JWT required)
| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| GET | `/api/v1/products?page=0&size=10` | USER/ADMIN | List products (paginated) |
| GET | `/api/v1/products/{id}` | USER/ADMIN | Get product by ID |
| POST | `/api/v1/products` | USER/ADMIN | Create product |
| PUT | `/api/v1/products/{id}` | USER/ADMIN | Update product |
| DELETE | `/api/v1/products/{id}` | ADMIN only | Delete product |
| GET | `/api/v1/products/{id}/items` | USER/ADMIN | Get items for product |

---

## Check Database

### Via Docker
```bash
docker exec -it zest-mysql mysql -u zestuser -pzestpass productdb
```
```sql
SHOW TABLES;
SELECT * FROM app_user;
SELECT * FROM product;
SELECT * FROM user_roles;
exit
```

### Via MySQL Workbench
| Field | Value |
|-------|-------|
| Host | `127.0.0.1` |
| Port | `3307` (Docker) or `3306` (Local) |
| Username | `zestuser` |
| Password | `zestpass` |
| Database | `productdb` |

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | MySQL host |
| `DB_PORT` | `3306` | MySQL port |
| `DB_NAME` | `productdb` | Database name |
| `DB_USERNAME` | `root` | DB username |
| `DB_PASSWORD` | `root` | DB password |
| `JWT_SECRET` | `ZestIndiaSecretKey...` | JWT signing secret |

---

## Running Tests
```bash
mvn test
```

---

## Evaluation Criteria Coverage

| Criteria | Implementation |
|----------|---------------|
| Code Structure | Clean layered architecture (controller → service → repo) |
| REST API Design | Resource-oriented, versioned (`/api/v1/`), paginated |
| Security | JWT + refresh token rotation, role-based authorization |
| Testing | Unit tests (Mockito) + Integration tests (H2) |
| Documentation | Swagger UI + this README |
| Docker | Dockerfile + Docker Compose |

---

## Copyright

© 2026 **Subham Kumar Sahani**. All rights reserved.

For any queries or feedback, feel free to reach out:

📧 **Email:** punitsahanirsrs1234@gmail.com

This project was developed as part of the Zest India assignment. Unauthorized copying or distribution of this project, via any medium, is strictly prohibited.