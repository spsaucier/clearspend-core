import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    id("org.springframework.boot") version "2.7.0"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("com.google.cloud.tools.jib") version "3.1.4"
    id("io.snyk.gradle.plugin.snykplugin") version "0.4"
    id("com.diffplug.spotless") version "6.6.1"
    id("net.ltgt.errorprone") version "2.0.2"
    jacoco
}

jacoco {
    toolVersion = "0.8.8"
}

group = "com.clearspend.capital"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_18
    targetCompatibility = JavaVersion.VERSION_18
}

jib {
    container {
      from.image = "openjdk:18.0.1-jdk-oracle"
      to.image = "capital/core"
      mainClass = "com.clearspend.capital.CapitalApplication"
        jvmFlags = listOf(
                "-server",
                "-Djava.awt.headless=true",
                "-XX:+UseG1GC",
                "-XX:MaxGCPauseMillis=100",
                "-XX:+UseStringDeduplication"
        )
    }
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
    compileJava {
        options.compilerArgs.add("-parameters")
    }

    compileTestJava {
        options.errorprone {
            disable("RestrictedApiChecker")
        }
    }

    test {
        jvmArgs(
            "-Dfile.encoding=UTF-8",
            "--add-opens=java.base/sun.security.x509=ALL-UNNAMED",
        )
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.errorprone.disableWarningsInGeneratedCode.set(true)
        options.errorprone.excludedPaths.set(".*/build/gen.*/.*")
        options.errorprone {
            disable("ParameterName") // https://github.com/google/error-prone/issues/1250
            disable("UnusedVariable") // https://github.com/google/error-prone/issues/1250
            disable("SameNameButDifferent") // https://github.com/google/error-prone/issues/2982
            disable("MissingOverride") // Lombok equals and hash code
            disable("DefaultCharset") // We set UTF-8 when the JVM starts
            disable("UnusedMethod") // https://github.com/google/error-prone/issues/3058
        }
    }

    withType<JacocoReport> {
        afterEvaluate {
            classDirectories.setFrom(files(classDirectories.files.map {
                fileTree(it).apply {
                    exclude("com/clearspend/capital/common/data/dao/**")
                    exclude("com/clearspend/capital/**/type/**")
                    exclude("com/clearspend/capital/**/types/**")
                    exclude("com/clearspend/capital/**/model/**")
                    exclude("com/clearspend/capital/common/error/**")
                    exclude("com/clearspend/capital/common/typedid/**")
                    exclude("com/clearspend/capital/crypto/data/**")
                    exclude("com/clearspend/capital/client/clearbit/**")
                    exclude("com/clearspend/capital/data/audit/**")
                    exclude("com/clearspend/capital/controller/nonprod/**")
                    exclude("com/clearspend/capital/client/codat/**")
                    exclude("com/clearspend/capital/cache/**")
                    exclude("com/clearspend/capital/client/stripe/**")
                    exclude("com/clearspend/capital/client/plaid/**")
                    exclude("com/clearspend/capital/client/google/**")
                }
            }))
        }
    }

    withType<JacocoCoverageVerification> {
        violationRules {
            rule {
                limit {
                    minimum = BigDecimal(0.76)
                    // This number should ideally be around .8 or .9, but not
                    // higher because of diminishing returns for the effort.
                    // It is set lower now so that we can more gracefully
                    // begin implementing more testing.
                    // Remember, too, that good coverage is not the same as good tests.
                }
            }
        }

        afterEvaluate {
            classDirectories.setFrom(files(classDirectories.files.map {
                fileTree(it).apply {
                    exclude("com/clearspend/capital/common/data/dao/**")
                    exclude("com/clearspend/capital/**/type/**")
                    exclude("com/clearspend/capital/**/types/**")
                    exclude("com/clearspend/capital/**/model/**")
                    exclude("com/clearspend/capital/common/error/**")
                    exclude("com/clearspend/capital/common/typedid/**")
                    exclude("com/clearspend/capital/crypto/data/**")
                    exclude("com/clearspend/capital/client/clearbit/**")
                    exclude("com/clearspend/capital/data/audit/**")
                    exclude("com/clearspend/capital/controller/nonprod/**")
                    exclude("com/clearspend/capital/client/codat/**")
                    exclude("com/clearspend/capital/cache/**")
                    exclude("com/clearspend/capital/client/stripe/**")
                    exclude("com/clearspend/capital/client/plaid/**")
                    exclude("com/clearspend/capital/client/google/**")
                }
            }))
        }
    }
}

dependencies {
    val testContainersVersion = "1.16.3"
    val blazePersistenceVersion = "1.6.6"
    val jmustacheVersion = "1.15"
    val jobrunrVersion = "5.1.1"

//annotation processor and dependencies
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    compileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    errorprone("com.google.errorprone:error_prone_core:2.13.1")

//spring boot starters
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-json")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

//3rd party libs managed by spring BOM
    implementation("org.apache.commons:commons-lang3")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("commons-codec:commons-codec")
    implementation("org.flywaydb:flyway-core")
    implementation("com.google.code.gson:gson")
    implementation("io.netty:netty-resolver-dns-native-macos:4.1.77.Final:osx-aarch_64")
    implementation("org.redisson:redisson-spring-boot-starter:3.17.2")
    runtimeOnly("org.postgresql:postgresql")

//other 3rd party libs
    implementation("com.google.guava:guava:31.0.1-jre")
    implementation("org.apache.commons:commons-csv:1.9.0")
    implementation("org.apache.commons:commons-text:1.9")
    implementation("org.springdoc:springdoc-openapi-ui:1.6.9")
    implementation("com.idealista:format-preserving-encryption:1.0.0")
    implementation("com.github.librepdf:openpdf:1.3.26")
    implementation("com.vladmihalcea:hibernate-types-55:2.14.0")
    implementation("com.blazebit:blaze-persistence-core-api:$blazePersistenceVersion")
    implementation("com.blazebit:blaze-persistence-core-impl:$blazePersistenceVersion")
    implementation("com.blazebit:blaze-persistence-integration-hibernate-5.6:$blazePersistenceVersion")
    implementation("com.samskivert:jmustache:$jmustacheVersion")
    implementation("com.jayway.jsonpath:json-path:2.7.0")
    implementation("com.google.cloud:google-cloud-bigtable:2.6.2")
    implementation("org.jobrunr:jobrunr-spring-boot-starter:$jobrunrVersion")

//client libs
    implementation("com.stripe:stripe-java:20.111.0") // from: https://github.com/stripe/stripe-java
    implementation("com.google.cloud:google-cloud-nio:0.124.2")
    implementation("com.sendgrid:sendgrid-java:4.8.3")
    implementation("com.plaid:plaid-java:11.4.0")
    implementation("com.twilio.sdk:twilio:8.25.1")
    implementation("io.fusionauth:fusionauth-java-client:1.36.0")
    implementation ("com.google.firebase:firebase-admin:8.1.0")


//monitoring support
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

//just for the non-prod data generator
    implementation("com.github.javafaker:javafaker:1.0.2") {
        exclude("org.yaml")
    }

// test section
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

//test containers
    testImplementation("org.testcontainers:postgresql:$testContainersVersion") {
        exclude("junit.junit")
    }
    testImplementation("org.testcontainers:junit-jupiter:$testContainersVersion") {
        exclude("junit.junit")
    }

    val permissionEnforcementProcessorJar = "lib/capital-permission-enforcement-1.0.0-SNAPSHOT.jar"
    implementation(files(permissionEnforcementProcessorJar))
    annotationProcessor(files(permissionEnforcementProcessorJar))
}
