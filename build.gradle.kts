plugins {
    java
    id("org.springframework.boot") version "4.0.1"
    id("org.graalvm.buildtools.native") version "0.10.6"
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
    implementation("org.springframework.ai:spring-ai-starter-mcp-server")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading", "-Xshare:off")
}
