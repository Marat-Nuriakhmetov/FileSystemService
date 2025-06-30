plugins {
    id("java")
    id("application")
}

group = "com.fileservice"
version = "1.0-SNAPSHOT"

sourceSets.main {
    resources.srcDirs("src/resources")
}

application {
    mainClass = "com.fileservice.Application"
}

repositories {
    mavenCentral()
    flatDir {
        dirs("libs")
    }
}

dependencies {
    implementation("com.github.arteam:simple-json-rpc-server:1.2")
    implementation("javax.servlet:javax.servlet-api:4.0.1")
    implementation("org.eclipse.jetty:jetty-server:11.0.18")
    implementation("org.eclipse.jetty:jetty-servlet:11.0.18")
    implementation("com.google.inject:guice:7.0.0")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("log4j:log4j:1.2.17")
    compileOnly("org.projectlombok:lombok:1.18.38")
    implementation("com.thetransactioncompany:jsonrpc2-client:2.1.1")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
