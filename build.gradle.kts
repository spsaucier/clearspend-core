plugins {
    java
    id("org.springframework.boot") version "2.5.4"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("org.flywaydb.flyway") version "8.0.0-beta1"
    id("com.google.cloud.tools.jib") version "3.1.4"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

jib {
    from.image = "openjdk:17-jdk-alpine3.13"
    to.image = "capital/core"
}

tasks {
    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}

dependencies {
    val springCloudVersion = "2020.0.3"

    //spring cloud BOM
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion"))

    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation("com.plaid:plaid-java:9.0.0")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("com.idealista:format-preserving-encryption:1.0.0")
    implementation("com.google.guava:guava:30.1.1-jre")
    implementation("org.apache.commons:commons-lang3:3.0")
    implementation("commons-codec:commons-codec:1.9")
    implementation("com.google.code.gson:gson:2.8.8")

    implementation("org.flywaydb:flyway-core")
    //to be used by feign as a http client
    implementation("com.squareup.okhttp3:okhttp")

    compileOnly("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.h2database:h2")
    testImplementation("com.github.javafaker:javafaker:1.0.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.1")
}
