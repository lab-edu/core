FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy Maven wrapper and project metadata first for better dependency cache reuse.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline

# Copy source code after dependencies are cached.
COPY src/ src/
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --create-home app
COPY --from=build --chown=app:app /app/target/*.jar app.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
