FROM maven:3.9.11-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .

COPY user-service/pom.xml user-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY delivery-service/pom.xml delivery-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml

RUN mvn -pl user-service -am dependency:go-offline -B

COPY user-service/src user-service/src

RUN mvn -pl user-service -am clean package -DskipTests


FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/user-service/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]