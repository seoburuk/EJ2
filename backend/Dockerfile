FROM maven:3.8.6-openjdk-8 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM tomcat:9-jdk8-openjdk

RUN rm -rf /usr/local/tomcat/webapps/*

COPY --from=build /app/target/ej2.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

CMD ["catalina.sh", "run"]
