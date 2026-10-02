# ============================================================
# Stage 1: Build
# ============================================================
FROM maven:3.9.11-eclipse-temurin-17 AS builder

WORKDIR /app

# Root Maven POM
COPY pom.xml .

# Module POMs
COPY user-service/pom.xml user-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY delivery-service/pom.xml delivery-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml

# Download dependencies
RUN mvn -pl api-gateway -am dependency:go-offline -B

# Copy only API Gateway source
COPY api-gateway/src api-gateway/src

# Build API Gateway and required reactor modules
RUN mvn -pl api-gateway -am clean package -DskipTests


# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/api-gateway/target/*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]