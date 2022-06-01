FROM openjdk:18.0.1.1-jdk-oraclelinux8
EXPOSE 8080
COPY build/libs/capital-*-SNAPSHOT.jar /capital.jar
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "/capital.jar"]
