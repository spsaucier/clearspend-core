FROM amazoncorretto:17.0.3-al2
EXPOSE 8080
COPY build/libs/capital-*-SNAPSHOT.jar /capital.jar
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "/capital.jar"]
