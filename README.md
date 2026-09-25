# ShopMart – Base Project (Java Microservice – Session 14)

Base project cho bài kiểm tra **"Nâng cấp phân hệ đặt hàng thành giao dịch phân tán (Saga Pattern)"**.

> Sau khi clone: **xoá thư mục `.git`**, sau đó `git init` và đẩy lên repository của bạn theo cú pháp `[Tên lớp]_[Họ Tên]` (ví dụ: `HN-K24-CNTT1_NguyenVanA`).

## 1. Công nghệ

| Thành phần | Phiên bản |
| --- | --- |
| Java | 17+ |
| Spring Boot | 3.3.5 |
| Spring Cloud | 2023.0.3 |
| MySQL | 8.x (Docker Compose) |
| Kafka (KRaft) / Redis | Docker Compose |

## 2. Cấu trúc project

```
shopmart-microservices
├── pom.xml                  # Parent POM (multi-module)
├── docker-compose.yml       # MySQL + Kafka (KRaft) + Redis
├── config-repo/             # File cấu hình cho Config Server (native)
│   ├── application.yml       # cấu hình chung (eureka, kafka, logging)
│   ├── order-service.yml
│   ├── inventory-service.yml
│   └── payment-service.yml
├── config-server/     :8888 # Câu 1
├── eureka-server/     :8761 # Câu 1
├── api-gateway/       :8080 # Câu 1
├── order-service/     :8081 # đơn hàng + Feign + CircuitBreaker + Saga orchestrator
├── inventory-service/ :8082 # tồn kho + Redis cache + Saga (trừ/hoàn kho)
└── payment-service/   :8083 # thanh toán (có giả lập lỗi) + Saga
```

## 3. Trình tự khởi động (QUAN TRỌNG)

Các service phụ thuộc nhau, phải chạy đúng thứ tự:

```
# 1. Hạ tầng: MySQL + Kafka + Redis
docker compose up -d

# 2. Build toàn bộ
mvn clean install

# 3. Chạy theo thứ tự (mỗi lệnh 1 terminal, hoặc Run trong IntelliJ)
mvn -pl config-server   spring-boot:run     # đợi tới khi log "Started ConfigServerApplication"
mvn -pl eureka-server    spring-boot:run     # http://localhost:8761
mvn -pl inventory-service spring-boot:run
mvn -pl payment-service   spring-boot:run
mvn -pl order-service     spring-boot:run
mvn -pl api-gateway       spring-boot:run     # http://localhost:8080
```

> Config Server phải chạy TRƯỚC các business service vì chúng nạp cấu hình từ `configserver:http://localhost:8888`.
> Chạy `config-server` từ thư mục gốc để `./config-repo` được nhận diện (native mode).

## 4. Đã hoàn thành 5 câu

| Câu | Nội dung | Vị trí chính |
| --- | --- | --- |
| 1 | Config Server (native) + Eureka + API Gateway (route `lb://`) | `config-server`, `eureka-server`, `api-gateway`, `config-repo` |
| 2 | FeignClient `inventory-service` + LoadBalancer + Resilience4j `@CircuitBreaker` + fallback | `order-service/client` |
| 3 | Kafka (KRaft) topic `order`, Choreography Saga + compensating (hoàn kho/huỷ đơn) | cả 3 service: `messaging/`, `saga/` |
| 4 | Redis + `@Cacheable`/`@CachePut`/`@CacheEvict` (Cache-Aside) | `inventory-service` |
| 5 | Clean code, không hard-code (Config Server), log SLF4J, unit test + test rollback | toàn project |

## 5. Kịch bản demo Saga

- **Happy path**: `POST /api/order {customerId:"C001", productId:1, quantity:2}`
  → ORDER_CREATED → INVENTORY_RESERVED → PAYMENT_COMPLETED → đơn `COMPLETED`.
- **Rollback (thanh toán lỗi)**: `POST /api/order {customerId:"C002", productId:3, quantity:3}`
  (3 × MacBook = 84.000.000 > hạn mức 80.000.000)
  → ORDER_CREATED → INVENTORY_RESERVED → **PAYMENT_FAILED** → đơn `CANCELLED` + INVENTORY_RELEASED (hoàn kho).
  Kiểm tra tồn kho productId=3 KHÔNG bị hụt sau khi rollback.

Postman: `postman/ShopMart.postman_collection.json`.

## 6. Chứng minh nhanh

- **Eureka**: mở http://localhost:8761 thấy 4 service (order/inventory/payment/api-gateway) UP.
- **Load Balancing**: chạy thêm instance inventory (`-Dserver.port=8084`), gọi `GET /api/order/products/1` nhiều lần, xem log 2 instance luân phiên.
- **Circuit Breaker**: tắt inventory-service, gọi `GET /api/order/products/1` → trả fallback; xem `GET /actuator/circuitbreakers` (order-service) thấy state OPEN.
- **Redis cache**: gọi `GET /api/inventory/products/1` lần 1 có log `Querying DB...`, lần 2 KHÔNG có (lấy từ Redis).
- **Test**: `mvn test` — có test rollback `OrderSagaListenerTest`.
