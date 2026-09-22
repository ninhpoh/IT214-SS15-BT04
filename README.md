# 🎵 Concert Orchestrator Saga với State Machine

## Mục tiêu

Xây dựng hệ thống đặt vé concert theo mô hình **Orchestration Saga**, trong đó một **Orchestrator/State Machine** tập trung quản lý trạng thái giao dịch và điều phối các Microservice.

---

## Kiến trúc tổng thể

```
┌──────────────────────────────────────────────────────────────────────┐
│                        CLIENT (Postman)                             │
│                   POST /api/bookings                                │
└──────────────────────┬───────────────────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────────────────┐
│                  ORCHESTRATOR SERVICE (port 8080)                    │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────────┐ │
│  │                    State Machine                                │ │
│  │                                                                 │ │
│  │  INITIATED ──► PAYMENT_PENDING ──► PAYMENT_COMPLETED            │ │
│  │                     │                    │                      │ │
│  │                PAYMENT_FAILED      SEAT_RESERVING               │ │
│  │                     │               │          │                │ │
│  │                  CANCELLED    RESERVATION   RESERVATION         │ │
│  │                              _SUCCESS      _FAILED              │ │
│  │                                  │            │                 │ │
│  │                           BOOKING_       CANCELLED              │ │
│  │                           CONFIRMED      + REFUND               │ │
│  └─────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌───────────────────┐         ┌─────────────────────────┐          │
│  │ PaymentOrch.Svc   │         │ ReservationOrch.Svc     │          │
│  │ (Retry: 3x, 2s)  │         │                         │          │
│  └────────┬──────────┘         └──────────┬──────────────┘          │
└───────────┼────────────────────────────────┼─────────────────────────┘
            │  REST                          │  REST
            ▼                                ▼
┌───────────────────────┐    ┌──────────────────────────────┐
│  PAYMENT SERVICE      │    │  CONCERT SERVICE             │
│  (port 8081)          │    │  (port 8082)                 │
│                       │    │                              │
│  POST /api/payments/  │    │  POST /api/concerts/reserve  │
│       process         │    │                              │
│  POST /api/payments/  │    │                              │
│       refund          │    │                              │
└───────────────────────┘    └──────────────────────────────┘
```

---

## Technology Stack

| Thành phần | Công nghệ |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Build Tool | Gradle |
| HTTP Client | RestClient |
| State Storage | ConcurrentHashMap (in-memory) |
| Logging | SLF4J + Logback |
| DTO | Lombok |

---

## Project Structure

```
bt04/
│
├── orchestrator-service/     (port 8080 - Nhạc trưởng)
│   ├── build.gradle
│   ├── settings.gradle
│   └── src/main/
│       ├── java/com/example/orchestrator/
│       │   ├── OrchestratorApplication.java
│       │   ├── controller/
│       │   │   └── BookingController.java
│       │   ├── model/
│       │   │   ├── BookingState.java          (Enum trạng thái)
│       │   │   ├── BookingEvent.java          (Enum sự kiện)
│       │   │   ├── BookingRequest.java        (DTO request)
│       │   │   └── BookingTransaction.java    (Lưu trạng thái)
│       │   ├── machine/
│       │   │   ├── ConcertBookingStateMachine.java      (Interface)
│       │   │   └── ConcertBookingStateMachineImpl.java  (Implementation)
│       │   ├── service/
│       │   │   ├── PaymentOrchestrationService.java     (Gọi Payment API)
│       │   │   └── ReservationOrchestrationService.java (Gọi Concert API)
│       │   ├── listener/
│       │   │   └── StateChangeListener.java   (Log state transitions)
│       │   └── config/
│       │       └── RestClientConfig.java      (Cấu hình RestClient)
│       └── resources/
│           └── application.yaml
│
├── payment-service/          (port 8081 - Xử lý thanh toán)
│   ├── build.gradle
│   ├── settings.gradle
│   └── src/main/
│       ├── java/com/example/payment/
│       │   ├── PaymentApplication.java
│       │   ├── controller/
│       │   │   └── PaymentController.java
│       │   ├── model/
│       │   │   └── PaymentRequest.java
│       │   └── service/
│       │       └── PaymentProcessingService.java
│       └── resources/
│           └── application.yaml
│
└── concert-service/          (port 8082 - Xử lý giữ chỗ)
    ├── build.gradle
    ├── settings.gradle
    └── src/main/
        ├── java/com/example/concert/
        │   ├── ConcertApplication.java
        │   ├── controller/
        │   │   └── ConcertController.java
        │   ├── model/
        │   │   └── ReservationRequest.java
        │   └── service/
        │       └── SeatReservationService.java
        └── resources/
            └── application.yaml
```

---

## State Machine

### Các trạng thái (BookingState)

| State | Mô tả |
|---|---|
| `INITIATED` | Booking vừa được tạo |
| `PAYMENT_PENDING` | Đang chờ thanh toán |
| `PAYMENT_COMPLETED` | Thanh toán thành công |
| `SEAT_RESERVING` | Đang giữ chỗ |
| `BOOKING_CONFIRMED` | Booking hoàn tất ✅ |
| `CANCELLED` | Booking bị huỷ ❌ |

### Các sự kiện (BookingEvent)

| Event | Mô tả |
|---|---|
| `PROCESS_PAYMENT` | Bắt đầu thanh toán |
| `PAYMENT_SUCCESS` | Thanh toán thành công |
| `PAYMENT_FAILED` | Thanh toán thất bại |
| `RESERVE_SEATS` | Bắt đầu giữ chỗ |
| `RESERVATION_SUCCESS` | Giữ chỗ thành công |
| `RESERVATION_FAILED` | Giữ chỗ thất bại |

### Bảng chuyển trạng thái (Transition Table)

| Current State | Event | New State |
|---|---|---|
| `INITIATED` | `PROCESS_PAYMENT` | `PAYMENT_PENDING` |
| `PAYMENT_PENDING` | `PAYMENT_SUCCESS` | `PAYMENT_COMPLETED` |
| `PAYMENT_PENDING` | `PAYMENT_FAILED` | `CANCELLED` |
| `PAYMENT_COMPLETED` | `RESERVE_SEATS` | `SEAT_RESERVING` |
| `SEAT_RESERVING` | `RESERVATION_SUCCESS` | `BOOKING_CONFIRMED` |
| `SEAT_RESERVING` | `RESERVATION_FAILED` | `CANCELLED` |

---

## Retry Policy

Áp dụng cho Payment:

| Thuộc tính | Giá trị |
|---|---|
| Maximum attempts | 3 |
| Delay | 2000ms |
| Retry khi | Network/Timeout Exception |
| Không retry khi | Business failure (success=false) |

---

## Compensation

Khi reservation thất bại sau khi payment đã thành công:

```
PAYMENT_COMPLETED → SEAT_RESERVING → RESERVATION_FAILED → CANCELLED → REFUND PAYMENT
```

Khi payment thất bại: **KHÔNG refund** (chưa có gì để hoàn).

---

## Hướng dẫn chạy

### Bước 1: Mở 3 Terminal trong IntelliJ

**Terminal 1 - Payment Service:**
```bash
cd payment-service
./gradlew bootRun
```

**Terminal 2 - Concert Service:**
```bash
cd concert-service
./gradlew bootRun
```

**Terminal 3 - Orchestrator Service:**
```bash
cd orchestrator-service
./gradlew bootRun
```

> **Quan trọng:** Phải chạy Payment Service và Concert Service **TRƯỚC** Orchestrator.

### Bước 2: Test bằng Postman

#### Case 1: Happy Path (Mặc định - tất cả thành công)

**POST** `http://localhost:8080/api/bookings`

**Body (raw JSON):**
```json
{
  "bookingId": "CONCERT-2026-088",
  "concertCode": "LIVE-HCM-2026-ULTRA",
  "customerId": "VIP-2024",
  "customerEmail": "rika@email.com",
  "ticketQuantity": 3,
  "amount": 5500000
}
```

**Expected Response:**
```json
{
  "bookingId": "CONCERT-2026-088",
  "concertCode": "LIVE-HCM-2026-ULTRA",
  "customerId": "VIP-2024",
  "customerEmail": "rika@email.com",
  "ticketQuantity": 3,
  "amount": 5500000.0,
  "currentState": "BOOKING_CONFIRMED",
  "message": "Booking confirmed successfully!"
}
```

**Expected Logs:**
```
[Orchestrator] State: INITIATED -> Event: PROCESS_PAYMENT -> New State: PAYMENT_PENDING
[Orchestrator] RetryPolicy: Activity 'processPayment' - Attempt 1/3
[PaymentService] Processing payment for booking: CONCERT-2026-088
[PaymentService] Payment completed successfully
[Orchestrator] State: PAYMENT_PENDING -> Event: PAYMENT_SUCCESS -> New State: PAYMENT_COMPLETED
[Orchestrator] State: PAYMENT_COMPLETED -> Event: RESERVE_SEATS -> New State: SEAT_RESERVING
[ConcertService] Reserving 3 seats for booking: CONCERT-2026-088
[ConcertService] Seats reserved successfully
[Orchestrator] State: SEAT_RESERVING -> Event: RESERVATION_SUCCESS -> New State: BOOKING_CONFIRMED
[Orchestrator] Final State: BOOKING_CONFIRMED for booking CONCERT-2026-088
```

---

#### Case 2: Payment Timeout (Test Retry Policy)

**Bước 2a:** Bật simulation timeout

**POST** `http://localhost:8081/api/payments/simulate/timeout?enable=true`

**Bước 2b:** Gửi booking request (giống Case 1)

**Expected:** Retry 3 lần, mỗi lần chờ 2 giây, sau đó CANCELLED.

**Bước 2c:** Tắt simulation

**POST** `http://localhost:8081/api/payments/simulate/timeout?enable=false`

---

#### Case 3: Reservation Failed + Refund (Test Compensation)

**Bước 3a:** Bật simulation failure cho Concert Service

**POST** `http://localhost:8082/api/concerts/simulate/failure?enable=true`

**Bước 3b:** Gửi booking request (giống Case 1)

**Expected Logs:**
```
[Orchestrator] State: INITIATED -> Event: PROCESS_PAYMENT -> New State: PAYMENT_PENDING
[Orchestrator] RetryPolicy: Activity 'processPayment' - Attempt 1/3
[PaymentService] Processing payment for booking: CONCERT-2026-088
[PaymentService] Payment completed successfully
[Orchestrator] State: PAYMENT_PENDING -> Event: PAYMENT_SUCCESS -> New State: PAYMENT_COMPLETED
[Orchestrator] State: PAYMENT_COMPLETED -> Event: RESERVE_SEATS -> New State: SEAT_RESERVING
[ConcertService] Reserving 3 seats for booking: CONCERT-2026-088
[ConcertService] Reservation FAILED for booking: CONCERT-2026-088 - Not enough seats
[Orchestrator] State: SEAT_RESERVING -> Event: RESERVATION_FAILED -> New State: CANCELLED
[Orchestrator] Compensation triggered for booking: CONCERT-2026-088
[PaymentService] Refund successfully for booking: CONCERT-2026-088
[Orchestrator] Final State: CANCELLED for booking CONCERT-2026-088
```

**Expected Response:**
```json
{
  "bookingId": "CONCERT-2026-088",
  "currentState": "CANCELLED",
  "message": "Booking cancelled - reservation failed. Payment refunded."
}
```

**Bước 3c:** Tắt simulation

**POST** `http://localhost:8082/api/concerts/simulate/failure?enable=false`

---

#### Case 4: Kiểm tra trạng thái booking

**GET** `http://localhost:8080/api/bookings/CONCERT-2026-088`

---

## API Endpoints

### Orchestrator Service (port 8080)

| Method | URL | Mô tả |
|---|---|---|
| POST | `/api/bookings` | Tạo booking mới |
| GET | `/api/bookings/{bookingId}` | Kiểm tra trạng thái |

### Payment Service (port 8081)

| Method | URL | Mô tả |
|---|---|---|
| POST | `/api/payments/process` | Xử lý thanh toán |
| POST | `/api/payments/refund` | Hoàn tiền |
| POST | `/api/payments/simulate/failure?enable=true/false` | Mô phỏng lỗi |
| POST | `/api/payments/simulate/timeout?enable=true/false` | Mô phỏng timeout |

### Concert Service (port 8082)

| Method | URL | Mô tả |
|---|---|---|
| POST | `/api/concerts/reserve` | Giữ chỗ |
| POST | `/api/concerts/simulate/failure?enable=true/false` | Mô phỏng lỗi |

---

## Tác giả

Bài thực hành IT-214 - Semester 15 - Bài tập 04
