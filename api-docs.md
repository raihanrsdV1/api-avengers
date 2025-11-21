# CareForAll Platform - API Documentation

This document provides a comprehensive reference for the CareForAll Platform APIs. All requests should be made through the API Gateway at `http://localhost:8080`.

## Base URL
`http://localhost:8080`

## Authentication
Currently, the platform uses a simplified authentication mechanism.
- **Header:** `X-User-Id` (Required for pledge creation)

---

## 1. Pledge Service
Manages pledge lifecycle and status.

### Create Pledge
Creates a new pledge for a campaign.

- **Endpoint:** `POST /api/v1/pledge`
- **Headers:**
  - `Content-Type: application/json`
  - `X-User-Id: <user_id>`
  - `X-Idempotency-Key: <unique_key>` (Optional but recommended)
- **Body:**
  ```json
  {
    "amount": 100.00,
    "campaignId": 1,
    "userId": 1
  }
  ```
- **Response (200 OK):**
  ```json
  {
    "id": 1,
    "amount": 100.00,
    "status": "CREATED",
    "campaignId": 1,
    "userId": 1,
    "paymentGatewayId": null
  }
  ```

### Get Pledge
Retrieves a pledge by ID.

- **Endpoint:** `GET /api/v1/pledge/{id}`
- **Response (200 OK):**
  ```json
  {
    "id": 1,
    "amount": 100.00,
    "status": "CAPTURED",
    "campaignId": 1,
    "userId": 1,
    "paymentGatewayId": "pg_12345"
  }
  ```

### Health Check
- **Endpoint:** `GET /api/v1/pledge/health`
- **Response (200 OK):** `UP`

---

## 2. Campaign Service
Manages campaigns and tracks total amounts.

### Get Campaign
Retrieves campaign details including current total raised.

- **Endpoint:** `GET /api/v1/campaign/{id}`
- **Response (200 OK):**
  ```json
  {
    "id": 1,
    "name": "Help the Kids",
    "description": "A campaign to help children in need",
    "targetAmount": 10000.00,
    "currentTotalAmount": 5500.00,
    "status": "ACTIVE"
  }
  ```

### Health Check
- **Endpoint:** `GET /api/v1/campaign/health`
- **Response (200 OK):** `UP`

---

## 3. Payment Service
Handles payment processing and webhooks.

### Webhook (Payment Status Update)
Receives status updates from the Payment Gateway.

- **Endpoint:** `POST /api/v1/payment/webhooks/status`
- **Headers:**
  - `Content-Type: application/json`
  - `X-Idempotency-Key: <unique_key>`
- **Body:**
  ```json
  {
    "paymentGatewayId": "pg_12345",
    "status": "CAPTURED",
    "timestamp": "2023-10-27T10:00:00Z"
  }
  ```
- **Response (200 OK):** `Webhook received`

### Health Check
- **Endpoint:** `GET /api/v1/payment/health`
- **Response (200 OK):** `UP`

---

## 4. Mock Payment Gateway
Simulates an external payment processor.

### Process Payment
Initiates a payment transaction.

- **Endpoint:** `POST /api/v1/mock-gateway/process`
- **Body:**
  ```json
  {
    "amount": 100.00,
    "currency": "USD",
    "source": "tok_visa",
    "callbackUrl": "http://payment-service:8081/api/v1/payment/webhooks/status"
  }
  ```
- **Response (202 Accepted):**
  ```json
  {
    "paymentGatewayId": "pg_12345",
    "status": "PROCESSING",
    "message": "Payment processing started. Webhook will be sent shortly."
  }
  ```

### Health Check
- **Endpoint:** `GET /api/v1/mock-gateway/health`
- **Response (200 OK):** `UP`

---

## Error Codes

| Status Code | Description |
| :--- | :--- |
| 400 | Bad Request - Invalid input or validation failure |
| 404 | Not Found - Resource does not exist |
| 409 | Conflict - Idempotency key collision or invalid state transition |
| 500 | Internal Server Error - Something went wrong on the server |
