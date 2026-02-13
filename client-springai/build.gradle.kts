plugins {
    java
    id("org.springframework.boot") version "4.0.1"
    application
}

group = "com.kousenit"
version = "1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.1"))
    implementation(platform("org.springframework.ai:spring-ai-bom:2.0.0-M2"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.ai:spring-ai-starter-mcp-client")
    implementation("info.picocli:picocli:4.7.5")
    implementation("tools.jackson.core:jackson-databind")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
}

application {
    mainClass = "com.kousenit.osqueryclient.springai.SpringAiOsqueryClientApplication"
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}
