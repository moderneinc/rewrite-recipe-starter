plugins {
    id("org.openrewrite.build.recipe-library-base") version "latest.release"

    // This uses the nexus publishing plugin to publish to the moderne-dev repository
    // Remove it if you prefer to publish by other means, such as the maven-publish plugin
    id("org.openrewrite.build.publish") version "latest.release"
    id("nebula.release") version "20.2.0" // Pinned as v21+ requires Gradle 9+

    // Configures artifact repositories used for dependency resolution to include maven central and nexus snapshots.
    // If you are operating in an environment where public repositories are not accessible, we recommend using a
    // virtual repository which mirrors both maven central and nexus snapshots.
    id("org.openrewrite.build.recipe-repositories") version "latest.release"
}

// Set as appropriate for your organization
group = "com.yourorg"
description = "Rewrite recipes."

dependencies {
    // The bom version can also be set to a specific version
    // https://github.com/openrewrite/rewrite-recipe-bom/releases
    implementation(platform("org.openrewrite.recipe:rewrite-recipe-bom:latest.release"))

    implementation("org.openrewrite:rewrite-java")
    implementation("org.openrewrite.recipe:rewrite-java-dependencies")
    implementation("org.openrewrite:rewrite-yaml")
    implementation("org.openrewrite:rewrite-xml")
    implementation("org.openrewrite.meta:rewrite-analysis")
    implementation("org.assertj:assertj-core:latest.release")
    runtimeOnly("org.openrewrite:rewrite-java-17")

    // Refaster style recipes need the rewrite-templating annotation processor and dependency for generated recipes
    // https://github.com/openrewrite/rewrite-templating/releases
    annotationProcessor("org.openrewrite:rewrite-templating:latest.release")
    implementation("org.openrewrite:rewrite-templating")
    // The `@BeforeTemplate` and `@AfterTemplate` annotations are needed for refaster style recipes
    compileOnly("com.google.errorprone:error_prone_core:latest.release") {
        exclude("com.google.auto.service", "auto-service-annotations")
        exclude("io.github.eisop","dataflow-errorprone")
    }

    // The RewriteTest class needed for testing recipes
    testImplementation("org.openrewrite:rewrite-test") {
        exclude(group = "org.slf4j", module = "slf4j-nop")
    }

    // Need to have a slf4j binding to see any output enabled from the parser.
    runtimeOnly("ch.qos.logback:logback-classic:1.2.+")

    // Our recipe converts Guava's `Lists` type
    testRuntimeOnly("com.google.guava:guava:latest.release")
    testRuntimeOnly("org.apache.commons:commons-lang3:latest.release")
    testRuntimeOnly("org.springframework:spring-core:latest.release")
    testRuntimeOnly("org.springframework:spring-context:latest.release")
}

signing {
    // To enable signing have your CI workflow set the "signingKey" and "signingPassword" Gradle project properties
    isRequired = false
}

// Use maven-style "SNAPSHOT" versioning for non-release builds
configure<nebula.plugin.release.git.base.ReleasePluginExtension> {
    defaultVersionStrategy = nebula.plugin.release.NetflixOssStrategies.SNAPSHOT(project)
}

configure<PublishingExtension> {
    publications {
        named("nebula", MavenPublication::class.java) {
            suppressPomMetadataWarningsFor("runtimeElements")
        }
    }
}

tasks.register("licenseFormat") {
    println("License format task not implemented for rewrite-recipe-starter")
}
