# Step 1: Build the JAR using Maven
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# Step 2: Run the Spring Boot application
FROM openjdk:17-jdk-slim
# JAR file name-ah unga target folder-la irukura maari check pannikonga
COPY --from=build /target/chakraEncryption2-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]