
# GWatcho Microservices App

A production-oriented microservices application built with
Java, Spring Boot, REST, Kafka, JPA/Hibernate and MySQL.

## Architecture

The application consists of:

- Product Service
- Order Service
- Delivery Service
- Kafka
- Transactional Outbox
- MySQL

## Current Implementation

### Product Service
- Product CRUD
- Optimistic locking
- Global exception handling
- Transactional Outbox
- Kafka publisher

### Order Service
- Order CRUD
- Checkout API
- Product snapshot
- Order/OrderItem persistence
- Transactional Outbox
- `OrderCreatedEvent`
- Kafka publisher
- Kafka E2E test

### Delivery Service
- Planned / in development

## Event Flow

Checkout:

Client
→ Order Service
→ Product Service
→ Order
→ Transactional Outbox
→ Kafka
→ Delivery Service

## Testing

The project contains:

- Unit tests
- Repository integration tests
- Service integration tests
- Kafka integration tests
- End-to-end tests

## Technologies

Java
Spring Boot
Spring Data JPA
Hibernate
MySQL
Kafka
Maven
JUnit 5
Mockito
AssertJ
MockMvc
