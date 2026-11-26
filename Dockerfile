FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /source
ARG SERVICE
COPY pom.xml ./
COPY verification/pom.xml verification/pom.xml
COPY platform/pom.xml platform/pom.xml
COPY identity-service/pom.xml identity-service/pom.xml
COPY ledger-service/pom.xml ledger-service/pom.xml
COPY vaults-service/pom.xml vaults-service/pom.xml
COPY reports-service/pom.xml reports-service/pom.xml
COPY platform/src platform/src
COPY identity-service/src identity-service/src
COPY ledger-service/src ledger-service/src
COPY vaults-service/src vaults-service/src
COPY reports-service/src reports-service/src
RUN mvn -B -ntp -DskipTests -pl "${SERVICE}" -am package

FROM eclipse-temurin:21-jre
WORKDIR /app
ARG SERVICE
COPY --from=build /source/${SERVICE}/target/${SERVICE}-1.0.0.jar /app/app.jar
EXPOSE 8081 8082 8083 8084
USER 10001
ENTRYPOINT ["java","-jar","/app/app.jar"]
