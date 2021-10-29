FROM openjdk:17-ea-22-jdk-oracle
EXPOSE 8080
COPY build/libs/capital-*-SNAPSHOT.jar /capital.jar
ENTRYPOINT ["java", "-jar", "/capital.jar"]
