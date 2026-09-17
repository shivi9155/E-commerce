# 🪔 Kanha Collection — Devotional E-Commerce Backend API

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/)
[![Java](https://img.shields.io/badge/Java-17-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-green)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red)](https://redis.io/)

Production-grade, highly available, secure, and scalable backend REST API for **Kanha Collection** — an Indian devotional e-commerce platform specializing in *Laddu Gopal dresses, brass idols, puja accessories, and spiritual decor*.

---

## 🚀 Key Features & Architectural Highlights

- **Robust Security Architecture**:
  - Stateless JWT Authentication with dual tokens (short-lived Access JWT + HTTP-Only SameSite Refresh Cookie).
  - Fine-grained Role-Based Access Control (`ROLE_USER`, `ROLE_ADMIN`).
  - Refresh token session revocation & blacklisting backed by **Redis**.
  - Rate limiting with bucket-algorithm protection against DDoS and brute-force attacks.

- **Inventory & Order Engine**:
  - Pessimistic DB locking (`SELECT FOR UPDATE`) on stock reservation during checkout.
  - Multi-status Order State Machine (`PENDING_PAYMENT` -> `CONFIRMED` -> `PROCESSING` -> `SHIPPED` -> `DELIVERED`).
  - Automatic inventory release upon order cancellation or timeout.
  - Indian PIN code deliverability checking with COD validation logic.

- **Payment Gateway Integration**:
  - Full **Razorpay** integration (Orders API, Signature HMAC SHA256 Verification, Async Webhooks).
  - Idempotent webhook processing using Redis event deduplication (`payment:webhook:event:{id}`).

- **Performance & Scalability**:
  - Multi-level caching (Redis second-level cache for Product Catalog & Category trees).
  - Flyway database schema migrations.
  - Comprehensive OpenApi 3.0 / Swagger documentation.

---

## 🛠️ Tech Stack & Dependencies

| Component | Technology |
|---|---|
| **Language & Framework** | Java 17, Spring Boot 3.2.3 |
| **Security & Auth** | Spring Security 6, JJWT (JSON Web Token), BCrypt |
| **Primary Database** | PostgreSQL 15 |
| **In-Memory Cache & Session** | Redis 7 (Spring Data Redis) |
| **Database Migrations** | Flyway DB |
| **Payment Gateway** | Razorpay Java SDK |
| **API Documentation** | Springdoc OpenAPI (Swagger UI) |
| **Containerization** | Docker, Docker Compose |

---

## 🚦 Getting Started

### Prerequisites
- JDK 17+
- Maven 3.8+
- Docker & Docker Compose (optional, for local setup)

### Environment Variables
Copy `.env.example` to `.env` or set environment values:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/kanhacollection_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
APP_JWT_SECRET=your_base64_encoded_super_secret_jwt_key_at_least_256_bits
APP_RAZORPAY_KEY_ID=rzp_test_your_key_id
APP_RAZORPAY_KEY_SECRET=your_razorpay_key_secret
```

### Running with Docker Compose (Recommended for local dev)
```bash
docker-compose up -d
```

### Manual Run
```bash
# Build the project
mvn clean package -DskipTests

# Run the application
java -jar target/backend-1.0.0.jar
```

---

## 📖 API Documentation & Swagger UI

Once the application is running, interact with live APIs at:
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

### Major Endpoint Categories
- `/auth/*` — Register, Login, Refresh Token, Logout, Password Management
- `/products/*` — Catalog Search, Filters, Category navigation, Variants
- `/cart/*` — Shopping cart management, stock availability check
- `/orders/*` — Customer checkout, order tracking, cancellation
- `/payments/*` — Razorpay order initiation, signature verification, webhooks
- `/coupons/*` — Coupon validation and discount calculations
- `/admin/*` — Admin order management, inventory updates, product management

---

## 🧪 Testing

Run unit tests and integration tests:
```bash
mvn clean test
```

---

## 📜 License
Internal proprietary software for Kanha Collection.
