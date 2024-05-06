## Rewrite recipe starter

This repository serves as a template for building your own recipe JARs and publishing them to a repository where they can be applied on [app.moderne.io](https://app.moderne.io) against all the public OSS code that is included there.

We've provided a sample recipe (NoGuavaListsNewArray) and a sample test class. Both of these exist as placeholders, and they should be replaced by whatever recipe you are interested in writing.

To begin, fork this repository and customize it by:

1. Changing the root project name in `settings.gradle.kts`.
2. Changing the `group` in `build.gradle.kts`.
3. Changing the package structure from `com.yourorg` to whatever you want.

## Getting started

Familiarize yourself with the [OpenRewrite documentation](https://docs.openrewrite.org/), in particular the [concepts & explanations](https://docs.openrewrite.org/concepts-explanations) op topics like the [lossless semantic trees](https://docs.openrewrite.org/concepts-explanations/lossless-semantic-trees), [recipes](https://docs.openrewrite.org/concepts-explanations/recipes) and [visitors](https://docs.openrewrite.org/concepts-explanations/visitors).

You might be interested to watch some of the [videos available on OpenRewrite and Moderne](https://www.youtube.com/@moderne-auto-remediation).

Once you want to dive into the code there is a [comprehensive getting started guide](https://docs.openrewrite.org/authoring-recipes/recipe-development-environment)
available in the OpenRewrite docs that provides more details than the below README.

## Reference recipes

* [META-INF/rewrite/stringutils.yml](./src/main/resources/META-INF/rewrite/stringutils.yml) - A declarative YAML recipe that replaces usages of `org.springframework.util.StringUtils` with `org.apache.commons.lang3.StringUtils`.
  - [UseApacheStringUtilsTest](./src/test/java/com/yourorg/UseApacheStringUtilsTest.java) - A test class for the `com.yourorg.UseApacheStringUtils` recipe.
* [NoGuavaListsNewArrayList.java](./src/main/java/com/yourorg/NoGuavaListsNewArrayList.java) - An imperative Java recipe that replaces usages of `com.google.common.collect.Lists` with `new ArrayList<>()`.
  - [NoGuavaListsNewArrayListTest.java](./src/test/java/com/yourorg/NoGuavaListsNewArrayListTest.java) - A test class for the `NoGuavaListsNewArrayList` recipe.
* [SimplifyTernary](./src/main/java/com/yourorg/SimplifyTernary.java) - An Refaster style recipe that simplifies ternary expressions.
  - [SimplifyTernaryTest](./src/test/java/com/yourorg/SimplifyTernaryTest.java) - A test class for the `SimplifyTernary` recipe.
* [AssertEqualsToAssertThat](./src/main/java/com/yourorg/AssertEqualsToAssertThat.java) - An imperative Java recipe that replaces JUnit's `assertEquals` with AssertJ's `assertThat`, to show how to handle classpath dependencies.
  - [AssertEqualsToAssertThatTest](./src/test/java/com/yourorg/AssertEqualsToAssertThatTest.java) - A test class for the `AssertEqualsToAssertThat` recipe.
* [AppendToReleaseNotes](./src/main/java/com/yourorg/AppendToReleaseNotes.java) - A ScanningRecipe that appends a message to the release notes of a project.
  - [AppendToReleaseNotesTest](./src/test/java/com/yourorg/AppendToReleaseNotesTest.java) - A test class for the `AppendToReleaseNotes` recipe.
* [ClassHierarchy](./src/main/java/com/yourorg/ClassHierarchy.java) - A recipe that demonstrates how to produce a data table on the class hierarchy of a project.
  - [ClassHierarchyTest](./src/test/java/com/yourorg/ClassHierarchyTest.java) - A test class for the `ClassHierarchy` recipe.
* [UpdateConcoursePipeline](./src/main/java/com/yourorg/UpdateConcoursePipeline.java) - A recipe that demonstrates how to update a Concourse pipeline, as an example of operating on Yaml files.
  - [UpdateConcoursePipelineTest](./src/test/java/com/yourorg/UpdateConcoursePipelineTest.java) - A test class for the `UpdateConcoursePipeline` recipe.

## Local Publishing for Testing

Before you publish your recipe module to an artifact repository, you may want to try it out locally.
To do this on the command line, run:
```bash
./gradlew publishToMavenLocal
# or ./gradlew pTML
# or mvn install
```
This will publish to your local maven repository, typically under `~/.m2/repository`.

Replace the groupId, artifactId, recipe name, and version in the below snippets with the ones that correspond to your recipe.

In the pom.xml of a different project you wish to test your recipe out in, make your recipe module a plugin dependency of rewrite-maven-plugin:
```xml
<project>
    <build>
        <plugins>
            <plugin>
                <groupId>org.openrewrite.maven</groupId>
                <artifactId>rewrite-maven-plugin</artifactId>
                <version>RELEASE</version>
                <configuration>
                    <activeRecipes>
                        <recipe>com.yourorg.NoGuavaListsNewArrayList</recipe>
                    </activeRecipes>
                </configuration>
                <dependencies>
                    <dependency>
                        <groupId>com.yourorg</groupId>
                        <artifactId>rewrite-recipe-starter</artifactId>
                        <version>0.1.0-SNAPSHOT</version>
                    </dependency>
                </dependencies>
            </plugin>
        </plugins>
    </build>
</project>
```

Unlike Maven, Gradle must be explicitly configured to resolve dependencies from Maven local.
The root project of your Gradle build, make your recipe module a dependency of the `rewrite` configuration:

```groovy
plugins {
    id("java")
    id("org.openrewrite.rewrite") version("latest.release")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    rewrite("com.yourorg:rewrite-recipe-starter:latest.integration")
}

rewrite {
    activeRecipe("com.yourorg.NoGuavaListsNewArrayList")
}
```

Now you can run `mvn rewrite:run` or `gradlew rewriteRun` to run your recipe.

## Publishing to Artifact Repositories

This project is configured to publish to Moderne's open artifact repository (via the `publishing` task at the bottom of
the `build.gradle.kts` file). If you want to publish elsewhere, you'll want to update that task.
[app.moderne.io](https://app.moderne.io) can draw recipes from the provided repository, as well as from [Maven Central](https://search.maven.org).

Note:
Running the publish task _will not_ update [app.moderne.io](https://app.moderne.io), as only Moderne employees can
add new recipes. If you want to add your recipe to [app.moderne.io](https://app.moderne.io), please ask the
team in [Slack](https://join.slack.com/t/rewriteoss/shared_invite/zt-nj42n3ea-b~62rIHzb3Vo0E1APKCXEA) or in [Discord](https://discord.gg/xk3ZKrhWAb).

These other docs might also be useful for you depending on where you want to publish the recipe:

* Sonatype's instructions for [publishing to Maven Central](https://maven.apache.org/repository/guide-central-repository-upload.html)
* Gradle's instructions on the [Gradle Publishing Plugin](https://docs.gradle.org/current/userguide/publishing\_maven.html).

### From Github Actions

The `.github` directory contains a Github action that will push a snapshot on every successful build.

Run the release action to publish a release version of a recipe.

### From the command line

To build a snapshot, run `./gradlew snapshot publish` to build a snapshot and publish it to Moderne's open artifact repository for inclusion at [app.moderne.io](https://app.moderne.io).

To build a release, run `./gradlew final publish` to tag a release and publish it to Moderne's open artifact repository for inclusion at [app.moderne.io](https://app.moderne.io).


## Applying OpenRewrite recipe development best practices

We maintain a collection of [best practices for writing OpenRewrite recipes](https://docs.openrewrite.org/recipes/recipes/openrewritebestpractices).
You can apply these recommendations to your recipes by running the following command:
```bash
./gradlew rewriteRun -Drewrite.activeRecipe=org.openrewrite.recipes.OpenRewriteBestPractices
```
or
```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:run -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-recommendations:RELEASE -Drewrite.activeRecipes=org.openrewrite.recipes.OpenRewriteBestPractices
```