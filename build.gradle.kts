plugins {
    id("java")
    id("application")
    id("com.gradleup.shadow") version "8.3.0"
}

group = "com.fileservice"
version = "1.0.0"

sourceSets.main {
    resources.srcDirs("src/resources")
}

tasks {
    jar {
        manifest {
            attributes["Main-Class"] = "com.fileservice.Application"
        }
    }
    shadowJar {
        archiveClassifier = "shadow"
        archiveFileName = "fileservice.jar"
        manifest {
            attributes["Main-Class"] = "com.fileservice.Application"
        }
    }
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
    compileOnly("org.projectlombok:lombok:1.18.38")
    implementation("com.thetransactioncompany:jsonrpc2-client:2.1.1")
    annotationProcessor("org.projectlombok:lombok:1.18.38")

    implementation("org.apache.logging.log4j:log4j-core:2.25.0")
    implementation("org.apache.logging.log4j:log4j-api:2.25.0")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.25.0")
    implementation("org.slf4j:slf4j-simple:2.0.17")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("org.mockito:mockito-core:4.5.1")
    testImplementation("org.mockito:mockito-junit-jupiter:4.5.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
}

tasks.test {
    useJUnitPlatform()
}
