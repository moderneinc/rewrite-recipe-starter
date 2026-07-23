plugins {
    id("org.openrewrite.build.recipe-library-base") version "latest.release"

    // Publishes to the moderne-dev repository via the nexus publishing plugin. It also
    // applies Nebula's MavenResolvedDependenciesPlugin, which pins dynamic versions
    // (e.g. "1.5.+", "latest.release") to the concrete versions Gradle resolved when
    // writing the published POM. If you replace this with the plain maven-publish plugin,
    // add `versionMapping { allVariants { fromResolutionResult() } }` to your publication --
    // otherwise dynamic versions are published verbatim and consumers resolving the recipe
    // through a private/virtual Maven repository may fail to resolve them.
    id("org.openrewrite.build.publish") version "latest.release"
    id("nebula.release") version "latest.release"

    // Configures artifact repositories used for dependency resolution to include maven central and nexus snapshots.
    // If you are operating in an environment where public repositories are not accessible, we recommend using a
    // virtual repository which mirrors both maven central and nexus snapshots.
    id("org.openrewrite.build.recipe-repositories") version "latest.release"
}

// Set as appropriate for your organization
group = "com.yourorg"
description = "Rewrite recipes."

// Code Genome Project (Moderne-hosted) repository for OpenRewrite and Moderne artifacts.
// Credentials are read from Gradle properties (e.g. ~/.gradle/gradle.properties) or the
// ORG_GRADLE_PROJECT_codegenomeUsername / ORG_GRADLE_PROJECT_codegenomePassword environment
// variables, and are intentionally kept out of source control.
repositories {
    maven {
        name = "codegenome"
        url = uri("https://artifacts.codegenomeproject.org/maven")
        val codegenomeUsername = providers.gradleProperty("codegenomeUsername").orNull
        val codegenomePassword = providers.gradleProperty("codegenomePassword").orNull
        if (codegenomeUsername != null && codegenomePassword != null) {
            credentials {
                username = codegenomeUsername
                password = codegenomePassword
            }
        }
    }
}

recipeDependencies {
    parserClasspath("org.jspecify:jspecify:1.0.0")
}

dependencies {
    // The bom version can also be set to a specific version
    // https://github.com/openrewrite/rewrite-recipe-bom/releases
    implementation(platform("org.openrewrite.recipe:rewrite-recipe-bom:latest.release"))

    implementation("org.openrewrite:rewrite-java")
    implementation("org.openrewrite.recipe:rewrite-java-dependencies")
    implementation("org.openrewrite:rewrite-yaml")
    implementation("org.openrewrite:rewrite-xml")
    implementation("org.openrewrite.meta:rewrite-analysis")

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
    testImplementation("org.assertj:assertj-core:latest.release")

    // Support for parsing different Java versions
    testRuntimeOnly("org.openrewrite:rewrite-java-17")
    testRuntimeOnly("org.openrewrite:rewrite-java-21")
    testRuntimeOnly("org.openrewrite:rewrite-java-25")

    // Need to have a slf4j binding to see any output enabled from the parser.
    runtimeOnly("ch.qos.logback:logback-classic:1.5.+")

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

        // If you replace `org.openrewrite.build.publish` above with the plain `maven-publish`
        // plugin, uncomment the block below so dynamic versions (e.g. "1.5.+", "latest.release")
        // are pinned to the concrete versions Gradle resolved when the POM is written. Without
        // it those selectors are published verbatim and consumers resolving the recipe through a
        // private/virtual Maven repository may fail to resolve them.
        //
        // withType<MavenPublication> {
        //     versionMapping {
        //         allVariants {
        //             fromResolutionResult()
        //         }
        //     }
        // }
    }
}

tasks.register("licenseFormat") {
    println("License format task not implemented for rewrite-recipe-starter")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Arewrite.javaParserClasspathFrom=resources")
}
