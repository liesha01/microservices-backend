# Java Spring Boot E-Commerce Microservices

A production-ready e-commerce system built with Spring Boot, Spring Cloud, and Docker featuring **Choreography-based Saga Pattern** for distributed transactions. 

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              API Gateway (8080)                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
        ┌──────────────────────────────┼──────────────────────────────┐
        │                              │                              │
        ▼                              ▼                              ▼
┌───────────────┐          ┌───────────────┐          ┌───────────────┐
│  Auth Service │          │ Order Service │          │Product Service│
│    (8081)     │          │    (8084)     │          │    (8083)     │
└───────────────┘          └───────┬───────┘          └───────────────┘
                                   │
                    ┌──────────────┴──────────────┐
                    │       Kafka Messages        │
                    └──────────────┬──────────────┘
                                   │
        ┌──────────────────────────┼──────────────────────────┐
        │                          │                          │
        ▼                          ▼                          ▼
┌───────────────┐          ┌───────────────┐          ┌───────────────┐
│Inventory Svc  │          │Payment Service│          │Notification   │
│    (8085)     │          │    (8086)     │          │Service (8087) │
└───────────────┘          └───────────────┘          └───────────────┘
```

## Services

| Service | Port | Description |
| :--- | :--- | :--- |
| **Config Server** | 8888 | Centralized Configuration |
| **Discovery Server** | 8761 | Eureka Service Registry |
| **API Gateway** | 8080 | Entry point, routing, load balancing |
| **Auth Service** | 8081 | Authentication (JWT), User/Role management |
| **User Service** | 8082 | User Profiles (First Name, Last Name, Address) |
| **Product Service** | 8083 | Product Catalog, Caching |
| **Order Service** | 8084 | Order Management, Saga Pattern initiator |
| **Inventory Service** | 8085 | Stock Management, Optimistic Locking |
| **Payment Service** | 8086 | Payment Processing (Simulated) |
| **Notification Service** | 8087 | Email/SMS Notifications (Simulated) |

## Saga Pattern Implementation

This project implements the **Choreography-based Saga Pattern** for handling distributed transactions across microservices.

### Saga Flow

```
1. Customer places order
        │
        ▼
┌───────────────────┐
│   Order Service   │──────► OrderPlacedEvent
│  (Status: PENDING)│
└───────────────────┘
        │
        ▼
┌───────────────────┐
│ Inventory Service │──────► InventoryReservedEvent (success)
│ (Reserve Stock)   │──────► InventoryReservationFailedEvent (failure)
└───────────────────┘
        │
        ▼ (on success)
┌───────────────────┐
│  Payment Service  │──────► PaymentProcessedEvent (success)
│ (Process Payment) │──────► PaymentFailedEvent (failure)
└───────────────────┘
        │
        ▼
┌───────────────────┐
│   Order Service   │
│ (Status: CONFIRMED│ ◄────── Success
│  or CANCELLED)    │ ◄────── Failure (compensating transaction)
└───────────────────┘
        │
        ▼
┌───────────────────┐
│Notification Service│
│(Send Notifications)│
└───────────────────┘
```

### Kafka Topics

| Topic | Publisher | Subscribers |
| :--- | :--- | :--- |
| `order-placed-topic` | Order Service | Inventory Service, Notification Service |
| `inventory-reserved-topic` | Inventory Service | Payment Service |
| `inventory-reservation-failed-topic` | Inventory Service | Order Service, Notification Service |
| `payment-processed-topic` | Payment Service | Order Service, Notification Service |
| `payment-failed-topic` | Payment Service | Order Service, Inventory Service, Notification Service |

### Compensating Transactions

- **Payment Failure**: Inventory Service restores reserved stock
- **Inventory Failure**: Order is cancelled immediately

## Technology Stack

- **Java 21** / Spring Boot 3.2.x
- **Spring Cloud** (Gateway, Config, Eureka, OpenFeign)
- **Database**: PostgreSQL (multi-tenant schema strategy via docker init)
- **Messaging**: Apache Kafka
- **Caching**: Redis
- **Containerization**: Docker & Docker Compose
- **Observability**: Zipkin

## How to Run

### Prerequisites
- Docker & Docker Compose installed
- Java 21+ (if running locally without Docker)
- Maven

### Steps

1.  **Clone the repository**
    ```bash
    git clone https://github.com/omerbzk/ecommerce-microservices.git
    cd ecommerce-microservices
    ```

2.  **Build all services**
    ```bash
    # Build each service (run in each service directory)
    for service in config-server discovery-server api-gateway auth-service user-service product-service order-service inventory-service payment-service notification-service; do
      echo "Building $service..."
      cd $service && mvn clean package -DskipTests && cd ..
    done
    ```

3.  **Start Infrastructure & Services**
    ```bash
    docker-compose up -d --build
    ```
    *This will spin up Postgres, Kafka, Redis, Zookeeper, Zipkin, and all microservices.*

4.  **Verify Services are Running**
    ```bash
    docker-compose ps
    ```

5.  **Access the Application**
    - Eureka Dashboard: http://localhost:8761
    - API Gateway: http://localhost:8080
    - Zipkin: http://localhost:9411

## Testing the Saga Flow

### 1. Add Inventory (via direct DB or API)
First, ensure you have inventory in the database. You can add test data:
```sql
INSERT INTO inventory (sku_code, quantity, version) VALUES ('iphone_15', 100, 0);
INSERT INTO inventory (sku_code, quantity, version) VALUES ('macbook_pro', 50, 0);
```

### 2. Register a User
```bash
curl -X POST http://localhost:8080/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{"username": "johndoe", "email": "john@example.com", "password": "password", "roles": ["user"]}'
```

### 3. Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username": "johndoe", "password": "password"}'
```
*Copy the `accessToken` from the response.*

### 4. Place an Order (Saga Flow Starts)
```bash
curl -X POST http://localhost:8080/api/orders \
     -H "Authorization: Bearer <YOUR_TOKEN>" \
     -H "Content-Type: application/json" \
     -d '{
       "userId": 1,
       "orderLineItems": [
         {"skuCode": "iphone_15", "price": 1200.00, "quantity": 2}
       ]
     }'
```

### 5. Check Order Status
```bash
curl http://localhost:8080/api/orders/<ORDER_NUMBER>
```

### 6. View Logs (See Saga in Action)
```bash
# View order-service logs
docker logs -f order-service

# View inventory-service logs
docker logs -f inventory-service

# View payment-service logs
docker logs -f payment-service

# View notification-service logs
docker logs -f notification-service
```

## Monitoring & Observability

### Zipkin Tracing
Access Zipkin at http://localhost:9411 to view distributed traces across services.

### Service Health Checks
Each service exposes health endpoints:
```bash
curl http://localhost:8084/api/orders/health  # Order Service
curl http://localhost:8086/api/payment/health # Payment Service
```

##  Configuration

### Environment Variables

| Variable | Default | Description |
| :--- | :--- | :--- |
| `DB_HOST` | localhost | PostgreSQL host |
| `DB_PORT` | 5432 | PostgreSQL port |
| `DB_USERNAME` | admin | Database username |
| `DB_PASSWORD` | password | Database password |
| `KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Kafka broker address |
| `EUREKA_SERVER_URL` | http://localhost:8761/eureka/ | Eureka server URL |
| `PAYMENT_SUCCESS_RATE` | 0.9 | Payment simulation success rate (0.0-1.0) |
| `PAYMENT_DELAY_MS` | 1000 | Payment processing delay simulation |

### Profiles
- **default**: Local development
- **docker**: Docker Compose deployment

## Project Structure

```
ecommerce-microservices/
├── api-gateway/
├── auth-service/
├── config-server/
├── discovery-server/
├── inventory-service/
├── notification-service/
├── order-service/
├── payment-service/
├── product-service/
├── user-service/
├── docker-compose.yml
├── init.sql
└── README.md
```

## Development Notes

### Simulated Services

**Payment Service**: Uses a configurable success rate to simulate payment outcomes. Set `PAYMENT_SUCCESS_RATE=1.0` for always-successful payments during testing.

**Notification Service**: Logs notifications to console. In production, integrate with:
- Email: SendGrid, AWS SES, Mailgun
- SMS: Twilio, AWS SNS
- Push: Firebase, OneSignal

### Adding Real Payment Integration

Replace the simulated payment logic in `PaymentService.java`:
```java
// Replace simulatePaymentProcessing() with actual API calls
private PaymentResult processWithStripe(String orderNumber, BigDecimal amount) {
    // Stripe integration code
}
```

## License

This project is licensed under the MIT License.

## 🤝 Contributing

Contributions are welcome! Please read the contributing guidelines before submitting a pull request.
