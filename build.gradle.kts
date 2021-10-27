plugins {
    java
    id("org.springframework.boot") version "2.5.5"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("org.flywaydb.flyway") version "8.0.0-beta1"
    id("com.google.cloud.tools.jib") version "3.1.4"
    id("io.snyk.gradle.plugin.snykplugin") version "0.4"
    id("com.diffplug.spotless") version "5.16.0"
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

spotless {
    java {
        googleJavaFormat()
    }
}

snyk {
    setApi("3becf623-120a-4d33-9b41-5573ff4c4f87")
    setAutoDownload(true)
    setAutoUpdate(true)
}

tasks {
    test {
        jvmArgs(
                "--add-opens=java.base/sun.security.x509=ALL-UNNAMED",
        )
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}

dependencies {
    val testContainersVersion = "1.16.0"

    //annotation processor and dependencies
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    compileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")

    //spring boot starters
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    //3rd party libs managed by spring BOM
    implementation("org.apache.commons:commons-lang3")
    implementation("commons-codec:commons-codec")
    implementation("org.flywaydb:flyway-core")
    implementation("com.google.code.gson:gson")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    runtimeOnly("org.postgresql:postgresql")

    //other 3rd party libs
    implementation("com.google.guava:guava:30.1.1-jre")
    implementation("org.springdoc:springdoc-openapi-ui:1.5.10")
    implementation("com.plaid:plaid-java:9.0.0")
    implementation("com.twilio.sdk:twilio:8.20.0")
    implementation("com.sendgrid:sendgrid-java:4.7.5")
    implementation("com.idealista:format-preserving-encryption:1.0.0")
    implementation("io.fusionauth:fusionauth-java-client:1.30.2")

    //just for the non-prod data generator
    implementation("com.github.javafaker:javafaker:1.0.1")

    // test section
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("com.github.javafaker:javafaker:1.0.1")
    testImplementation("org.mock-server:mockserver-netty:5.11.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    //test containers
    testImplementation("org.testcontainers:postgresql:$testContainersVersion") {
        exclude("junit.junit")
    }
    testImplementation("org.testcontainers:junit-jupiter:$testContainersVersion") {
        exclude("junit.junit")
    }
}
