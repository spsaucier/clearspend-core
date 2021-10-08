FROM openjdk:17-jdk-alpine3.13
EXPOSE 8080
COPY build/libs/capital-*-SNAPSHOT.jar /capital.jar
ENTRYPOINT ["java", "-jar", "/capital.jar"]
