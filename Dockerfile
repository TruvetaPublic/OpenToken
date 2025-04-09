#############################################
# Stage 1: Build the application
#############################################
ARG JAVA_VERSION=11
ARG MAVEN_VERSION=3.8.7

FROM maven:${MAVEN_VERSION}-amazoncorretto-${JAVA_VERSION} AS build

RUN mkdir /app
WORKDIR /app
COPY . /app

COPY src /app/src
COPY pom.xml /app/pom.xml

RUN mvn clean package

#############################################
# Stage 2: Create the image to run the application
#############################################
FROM amazoncorretto:11-alpine AS final

RUN mkdir /app

RUN addgroup --system appuser && adduser --system --no-create-home --ingroup appuser appuser

ARG VERSION=1.5.2
COPY --from=build /app/target/open-token-${VERSION}.jar /usr/local/lib/open-token.jar

WORKDIR /app

RUN chown -R appuser:appuser /app
USER appuser

ENTRYPOINT ["java", "-jar", "/usr/local/lib/open-token.jar"]