FROM gradle:8.9-jdk21 AS build
WORKDIR /src
COPY settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY gradlew gradlew.bat ./
COPY src ./src
RUN gradle --no-daemon installDist -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/build/install/vdvoem-server ./
EXPOSE 8080
ENV PORT=8080
ENTRYPOINT ["/app/bin/vdvoem-server"]