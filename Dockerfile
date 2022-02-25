FROM openjdk:17.0.2-jdk-oracle
EXPOSE 8080
COPY build/libs/capital-*-SNAPSHOT.jar /capital.jar
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "/capital.jar"]
