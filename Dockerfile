FROM eclipse-temurin:17-jdk-jammy as build
WORKDIR /app
COPY . .
RUN ./gradlew clean build -x test

FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]