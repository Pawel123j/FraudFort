plugins {
    // Auto-provisions the Java 21 toolchain (downloads it if the local machine
    // doesn't have a matching JDK), so the build is reproducible regardless of
    // the developer's installed Java version. See ADR 0002.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "orchestrator"
