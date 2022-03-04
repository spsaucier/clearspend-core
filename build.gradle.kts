import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    id("org.springframework.boot") version "2.6.3"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("com.google.cloud.tools.jib") version "3.1.4"
    id("io.snyk.gradle.plugin.snykplugin") version "0.4"
    id("com.diffplug.spotless") version "5.16.0"
    id("net.ltgt.errorprone") version "2.0.2"
    jacoco
}

group = "com.clearspend.capital"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

jib {
    from.image = "openjdk:17.0.2-jdk-oracle"
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
    compileJava {
        options.compilerArgs.add("-parameters")
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
                    exclude("com/clearspend/capital/client/clearbit/response/**")
                }
            }))
        }
    }

    withType<JacocoCoverageVerification> {
        violationRules {
            rule {
                limit {
                    minimum = BigDecimal(0.66)
                    // This number should ideally be around .8 or .9, but not
                    // higher because of diminishing returns for the effort.
                    // It is set lower now so that we canmore gracefully
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
                    exclude("com/clearspend/capital/client/clearbit/response/**")
                }
            }))
        }
    }
}

dependencies {
    val testContainersVersion = "1.16.3"
    val blazePersistenceVersion = "1.6.6"

//annotation processor and dependencies
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    compileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    errorprone("com.google.errorprone:error_prone_core:2.11.0")

//spring boot starters
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-json")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

//3rd party libs managed by spring BOM
    implementation("org.apache.commons:commons-lang3")
    implementation("commons-codec:commons-codec")
    implementation("org.flywaydb:flyway-core")
    implementation("com.google.code.gson:gson")
    runtimeOnly("org.postgresql:postgresql")

//other 3rd party libs
    implementation("com.google.guava:guava:31.0.1-jre")
    implementation("org.apache.commons:commons-csv:1.9.0")
    implementation("org.springdoc:springdoc-openapi-ui:1.6.5")
    implementation("com.idealista:format-preserving-encryption:1.0.0")
    implementation("com.github.librepdf:openpdf:1.3.26")
    implementation("com.vladmihalcea:hibernate-types-55:2.14.0")
    implementation("com.blazebit:blaze-persistence-core-api:$blazePersistenceVersion")
    implementation("com.blazebit:blaze-persistence-core-impl:$blazePersistenceVersion")
    implementation("com.blazebit:blaze-persistence-integration-hibernate-5.6:$blazePersistenceVersion")

//client libs
    implementation("com.stripe:stripe-java:20.94.0") // from: https://github.com/stripe/stripe-java
    implementation("com.google.cloud:google-cloud-nio:0.123.20")
    implementation("com.sendgrid:sendgrid-java:4.8.3")
    implementation("com.plaid:plaid-java:9.0.0")
    implementation("com.twilio.sdk:twilio:8.25.1")
    implementation("io.fusionauth:fusionauth-java-client:1.30.2")

//snyk fixes (revisit if fixed in the next spring boot version > 2.6.3)
    constraints {
        implementation("org.postgresql:postgresql:24.3.2") {
            because("https://security.snyk.io/vuln/SNYK-JAVA-COMFASTERXMLJACKSONDATATYPE-173759")
        }
    }

//monitoring support
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

//just for the non-prod data generator
    implementation("com.github.javafaker:javafaker:1.0.1")

// test section
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("com.github.javafaker:javafaker:1.0.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

//test containers
    testImplementation("org.testcontainers:postgresql:$testContainersVersion") {
        exclude("junit.junit")
    }
    testImplementation("org.testcontainers:junit-jupiter:$testContainersVersion") {
        exclude("junit.junit")
    }
}
