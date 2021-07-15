## Rewrite recipe starter

This repository serves as a template for building your own recipe JARs and publishing them to a repository where they can be applied on [app.moderne.io](https://app.moderne.io) against all of the public OSS code that is included there.

We include a sample recipe and test that just exists as a placeholder and is intended to be replaced by whatever recipe you are interested in writing.

Fork this repository and customize by:

1. Change the root project name in `settings.gradle.kts`.
2. Change the `group` in `build.gradle.kts`.
3. Change the package structure from `org.openrewrite` to whatever you want.

## To release your recipe artifact

To build a snapshot, run `./gradlew snapshot` to build a snapshot and publish it to Moderne's open artifact repository for inclusion at [app.moderne.io](https://app.moderne.io).

To build a release, run `./gradlew final` to tag a release and publish it to Moderne's open artifact repository for inclusion at [app.moderne.io](https://app.moderne.io).
