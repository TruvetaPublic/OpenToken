##################################################
# Stage 1: Build the application
##################################################
ARG JAVA_VERSION=11
ARG MAVEN_VERSION=3.8.8

FROM maven:${MAVEN_VERSION}-amazoncorretto-${JAVA_VERSION} AS build

RUN mkdir /app
WORKDIR /app

COPY lib/java /app
COPY resources /resources

RUN mvn clean package

##################################################
# Stage 2: Create the image to run the application
##################################################
FROM amazoncorretto:11-alpine AS final

RUN mkdir /app

RUN addgroup --system appuser && adduser --system --no-create-home --ingroup appuser appuser

ARG VERSION=1.11.0
COPY --from=build /app/target/opentoken-${VERSION}.jar /usr/local/lib/opentoken.jar

WORKDIR /app

RUN chown -R appuser:appuser /app
USER appuser

ENTRYPOINT ["java", "-jar", "/usr/local/lib/opentoken.jar"]