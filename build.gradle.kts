plugins {
    id("org.openrewrite.build.recipe-library") version "latest.release"
}

// Set as appropriate for your organization
group = "com.yourorg"
description = "Rewrite recipes."

// The bom version can also be set to a specific version or latest.release.
val rewriteVersion = "latest.integration"
dependencies {
    implementation(platform("org.openrewrite.recipe:rewrite-recipe-bom:${rewriteVersion}"))

    implementation("org.openrewrite:rewrite-java")
    runtimeOnly("org.openrewrite:rewrite-java-17")

    // Refaster style recipes need the annotation processor and dependencies
    annotationProcessor("org.openrewrite:rewrite-templating:${rewriteVersion}")
    implementation("org.openrewrite:rewrite-templating")
    compileOnly("com.google.errorprone:error_prone_core:2.19.1:with-dependencies") {
        exclude("com.google.auto.service", "auto-service-annotations")
    }

    // Need to have a slf4j binding to see any output enabled from the parser.
    runtimeOnly("ch.qos.logback:logback-classic:1.2.+")

    testRuntimeOnly("com.google.guava:guava:latest.release")
}

configure<PublishingExtension> {
    publications {
        named("nebula", MavenPublication::class.java) {
            suppressPomMetadataWarningsFor("runtimeElements")
        }
    }
}

publishing {
  repositories {
      maven {
          name = "moderne"
          url = uri("https://us-west1-maven.pkg.dev/moderne-dev/moderne-recipe")
      }
  }
}
