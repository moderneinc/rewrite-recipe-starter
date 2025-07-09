package com.yourorg;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;

import static org.openrewrite.test.SourceSpecs.text;
import static org.openrewrite.xml.Assertions.xml;
import static org.openrewrite.gradle.Assertions.buildGradle;

public class UseSdkManJavaVersionTest implements RewriteTest {
    
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseSdkManJavaVersion());
    }

    @DocumentExample
    @Test
    void createSdkmanrcFromMavenMultiModuleProject() {
        rewriteRun(
          // Root pom.xml with Java 17
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                                           http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0.0</version>
                  <packaging>pom</packaging>
                  <properties>
                      <maven.compiler.source>17</maven.compiler.source>
                      <maven.compiler.target>17</maven.compiler.target>
                  </properties>
                  <modules>
                      <module>module-a</module>
                      <module>module-b</module>
                  </modules>
              </project>
              """,
            spec -> spec.path(Path.of("pom.xml"))
          ),
          
          // Module A pom.xml (inherits Java 17 from parent)
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                                           http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                      <groupId>com.example</groupId>
                      <artifactId>parent</artifactId>
                      <version>1.0.0</version>
                  </parent>
                  <artifactId>module-a</artifactId>
              </project>
              """,
            spec -> spec.path(Path.of("module-a/pom.xml"))
          ),
          
          // Module B pom.xml (overrides with Java 11)
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                                           http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                      <groupId>com.example</groupId>
                      <artifactId>parent</artifactId>
                      <version>1.0.0</version>
                  </parent>
                  <artifactId>module-b</artifactId>
                  <properties>
                      <maven.compiler.source>11</maven.compiler.source>
                      <maven.compiler.target>11</maven.compiler.target>
                  </properties>
              </project>
              """,
            spec -> spec.path(Path.of("module-b/pom.xml"))
          ),
          
          // Expected .sdkmanrc file should be created with the highest Java version found
          text(
            null,
            """
              # Java versions detected across modules:
              # Root: 17
              # module-a: 17 (inherited)
              # module-b: 11
              # Using highest version found: 17
              java=17.0.0
              """,
            spec -> spec.path(Path.of(".sdkmanrc"))
          )
        );
    }

    @Test
    void createSdkmanrcFromGradleMultiModuleProject() {
        rewriteRun(
          // Root build.gradle with Java 21
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              
              allprojects {
                  java {
                      toolchain {
                          languageVersion = JavaLanguageVersion.of(21)
                      }
                  }
              }
              
              subprojects {
                  apply plugin: 'java'
              }
              """,
            spec -> spec.path(Path.of("build.gradle"))
          ),
          
          // settings.gradle
          text(
            """
              rootProject.name = 'multi-module-project'
              include ':service-a'
              include ':service-b'
              """,
            spec -> spec.path(Path.of("settings.gradle"))
          ),
          
          // Service A build.gradle (inherits Java 21)
          buildGradle(
            """
              // Inherits Java 21 from root
              dependencies {
                  implementation 'org.springframework:spring-core:5.3.21'
              }
              """,
            spec -> spec.path(Path.of("service-a/build.gradle"))
          ),
          
          // Service B build.gradle (overrides with Java 17)
          buildGradle(
            """
              java {
                  toolchain {
                      languageVersion = JavaLanguageVersion.of(17)
                  }
              }
              
              dependencies {
                  implementation 'org.springframework:spring-web:5.3.21'
              }
              """,
            spec -> spec.path(Path.of("service-b/build.gradle"))
          ),
          
          // Expected .sdkmanrc file
          text(
            null,
            """
              # Java versions detected across modules:
              # Root: 21
              # service-a: 21 (inherited)
              # service-b: 17
              # Using highest version found: 21
              java=21.0.0
              """,
            spec -> spec.path(Path.of(".sdkmanrc"))
          )
        );
    }

    @Test
    void updateExistingSdkmanrcFile() {
        rewriteRun(
          // Maven project with Java 17
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                                           http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <properties>
                      <maven.compiler.source>17</maven.compiler.source>
                      <maven.compiler.target>17</maven.compiler.target>
                  </properties>
              </project>
              """,
            spec -> spec.path(Path.of("pom.xml"))
          ),
          
          // Existing .sdkmanrc with older Java version
          text(
            """
              # Generated by previous run
              java=11.0.0
              maven=3.8.6
              """,
            """
              # Generated by previous run
              # Java versions detected across modules:
              # Root: 17
              # Using highest version found: 17
              java=17.0.0
              maven=3.8.6
              """,
            spec -> spec.path(Path.of(".sdkmanrc"))
          )
        );
    }

    @Test
    void detectJavaVersionFromMavenCompilerPlugin() {
        rewriteRun(
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                                           http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <version>3.11.0</version>
                              <configuration>
                                  <source>19</source>
                                  <target>19</target>
                              </configuration>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            spec -> spec.path(Path.of("pom.xml"))
          ),
          
          text(
            null,
            """
              # Java versions detected across modules:
              # Root: 19
              # Using highest version found: 19
              java=19.0.0
              """,
            spec -> spec.path(Path.of(".sdkmanrc"))
          )
        );
    }

    @Test
    void detectJavaVersionFromGradleCompileOptions() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              
              compileJava {
                  sourceCompatibility = '20'
                  targetCompatibility = '20'
              }
              """,
            spec -> spec.path(Path.of("build.gradle"))
          ),
          
          text(
            null,
            """
              # Java versions detected across modules:
              # Root: 20
              # Using highest version found: 20
              java=20.0.0
              """,
            spec -> spec.path(Path.of(".sdkmanrc"))
          )
        );
    }

    @Test
    void mixedBuildSystemsGradleAndMaven() {
        rewriteRun(
          // Root Gradle project
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              
              java {
                  toolchain {
                      languageVersion = JavaLanguageVersion.of(17)
                  }
              }
              """,
            spec -> spec.path(Path.of("build.gradle"))
          ),
          
          // Maven submodule (legacy component)
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                                           http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>legacy-service</artifactId>
                  <version>1.0.0</version>
                  <properties>
                      <maven.compiler.source>11</maven.compiler.source>
                      <maven.compiler.target>11</maven.compiler.target>
                  </properties>
              </project>
              """,
            spec -> spec.path(Path.of("legacy-service/pom.xml"))
          ),
          
          text(
            null,
            """
              # Java versions detected across modules:
              # Root: 17
              # legacy-service: 11
              # Using highest version found: 17
              java=17.0.0
              """,
            spec -> spec.path(Path.of(".sdkmanrc"))
          )
        );
    }

    @Test
    void noChangeWhenSdkmanrcAlreadyCorrect() {
        rewriteRun(
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                                           http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <properties>
                      <maven.compiler.source>17</maven.compiler.source>
                      <maven.compiler.target>17</maven.compiler.target>
                  </properties>
              </project>
              """,
            spec -> spec.path(Path.of("pom.xml"))
          ),
          
          // .sdkmanrc already has correct Java version
          text(
            """
              # Java versions detected across modules:
              # Root: 17
              # Using highest version found: 17
              java=17.0.0
              maven=3.9.4
              """,
            spec -> spec.path(Path.of(".sdkmanrc"))
          )
        );
    }

    @Test
    void complexMultiModuleWithDifferentJavaVersions() {
        rewriteRun(
          // Root pom.xml (parent)
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                                           http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0.0</version>
                  <packaging>pom</packaging>
                  <properties>
                      <maven.compiler.source>17</maven.compiler.source>
                      <maven.compiler.target>17</maven.compiler.target>
                  </properties>
                  <modules>
                      <module>core</module>
                      <module>web</module>
                      <module>legacy</module>
                  </modules>
              </project>
              """,
            spec -> spec.path(Path.of("pom.xml"))
          ),
          
          // Core module (inherits 17)
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                                           http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                      <groupId>com.example</groupId>
                      <artifactId>parent</artifactId>
                      <version>1.0.0</version>
                  </parent>
                  <artifactId>core</artifactId>
              </project>
              """,
            spec -> spec.path(Path.of("core/pom.xml"))
          ),
          
          // Web module (upgrades to 21)
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                                           http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                      <groupId>com.example</groupId>
                      <artifactId>parent</artifactId>
                      <version>1.0.0</version>
                  </parent>
                  <artifactId>web</artifactId>
                  <properties>
                      <maven.compiler.source>21</maven.compiler.source>
                      <maven.compiler.target>21</maven.compiler.target>
                  </properties>
              </project>
              """,
            spec -> spec.path(Path.of("web/pom.xml"))
          ),
          
          // Legacy module (downgrades to 11)
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                                           http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                      <groupId>com.example</groupId>
                      <artifactId>parent</artifactId>
                      <version>1.0.0</version>
                  </parent>
                  <artifactId>legacy</artifactId>
                  <properties>
                      <maven.compiler.source>11</maven.compiler.source>
                      <maven.compiler.target>11</maven.compiler.target>
                  </properties>
              </project>
              """,
            spec -> spec.path(Path.of("legacy/pom.xml"))
          ),
          
          // Expected .sdkmanrc should use highest version (21)
          text(
            null,
            """
              # Java versions detected across modules:
              # Root: 17
              # core: 17 (inherited)
              # web: 21
              # legacy: 11
              # Using highest version found: 21
              java=21.0.0
              """,
            spec -> spec.path(Path.of(".sdkmanrc"))
          )
        );
    }
}
