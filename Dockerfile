FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/Agile_mid_practise-1.0
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
