plugins {
    id("org.openrewrite.build.recipe-library") version "latest.release"
}

// Set as appropriate for your organization
group = "com.yourorg"
description = "Rewrite recipes."

dependencies {
    // The bom version can also be set to a specific version
    // https://github.com/openrewrite/rewrite-recipe-bom/releases
    implementation(platform("org.openrewrite.recipe:rewrite-recipe-bom:latest.release"))

    implementation("org.openrewrite:rewrite-java")
    runtimeOnly("org.openrewrite:rewrite-java-17")

    // Refaster style recipes need the rewrite-templating annotation processor and dependency for generated recipes
    // https://github.com/openrewrite/rewrite-templating/releases
    annotationProcessor("org.openrewrite:rewrite-templating:latest.release")
    implementation("org.openrewrite:rewrite-templating")
    // The `@BeforeTemplate` and `@AfterTemplate` annotations are needed for refaster style recipes
    compileOnly("com.google.errorprone:error_prone_core:2.19.1") {
        exclude("com.google.auto.service", "auto-service-annotations")
    }

    // Need to have a slf4j binding to see any output enabled from the parser.
    runtimeOnly("ch.qos.logback:logback-classic:1.2.+")

    // Our recipe converts Guava's `Lists` type
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
