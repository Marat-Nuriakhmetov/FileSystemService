plugins {
    id("java")
    id("application")
}

group = "jb.test"
version = "1.0-SNAPSHOT"

sourceSets.main {
    resources.srcDirs("src/resources")
}

application {
    mainClass = "jb.test.Main"
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
