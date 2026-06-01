# Stage 1: Build the Application
FROM maven:3-eclipse-temurin-11 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
# Build the WAR file using maven, ignore running tests in build container to speed up builds (tests run in CI pipelines natively).
RUN mvn clean package -DskipTests

# Stage 2: Deploy to Tomcat 9
FROM tomcat:9-jdk11
# Erase the default tomcat example webapps to reduce attack surface
RUN rm -rf /usr/local/tomcat/webapps/*

# Place the compiled war from the build container directly into the webapps directory as ROOT (default app context)
# Disable Tomcat shutdown port to stop health check warnings on Render
RUN sed -i 's/port="8005"/port="-1"/g' /usr/local/tomcat/conf/server.xml

COPY --from=build /app/target/olms.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]
