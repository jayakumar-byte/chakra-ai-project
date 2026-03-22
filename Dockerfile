# Step 1: Build the JAR using Maven
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# Step 2: Run the Spring Boot application
# openjdk-ku bathila indha stable image use pannunga
FROM eclipse-temurin:17-jdk-alpine
# JAR file path-ah wild card (*) vachu simplify pannidalam
COPY --from=build /target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]