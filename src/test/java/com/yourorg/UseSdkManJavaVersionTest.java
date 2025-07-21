package com.yourorg;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Path;

import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.test.SourceSpecs.text;

class UseSdkManJavaVersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseSdkManJavaVersion());
    }

    @DocumentExample
    @Test
    void createSdkmanrcFromMultipleJavaVersions() {
        rewriteRun(
          mavenProject("test",
            srcMainJava(
              version(
                java(
                  """
                    package com.example.core;
                    
                    class CoreService {
                        void processData() {
                            System.out.println("Processing data...");
                        }
                    }
                    """
                ), 17)
            ),
            srcTestJava(
              version(
                java(
                  """
                    package com.example.web;
                    
                    class WebService {
                        void serveRequests() {
                            System.out.println("Serving requests...");
                        }
                    }
                    """
                ), 21)
            )
          ),
          // Expected .sdkmanrc file should be created with the highest Java version found
          text(
            doesNotExist(),
            """
              java=21
              """,
            sourceSpecs -> sourceSpecs.path(Path.of(".sdkmanrc"))
          )
        );
    }

    @Test
    void updateExistingSdkmanrc() {
        rewriteRun(
          mavenProject(
            "test",
            srcMainJava(
              version(
                java(
                  """
                    package com.example;
                    
                    class Service {
                        void doWork() {
                            System.out.println("Working...");
                        }
                    }
                    """
                ), 17)
            )
          ),
          // Existing .sdkmanrc with older Java version
          text(
            """
              java=11
              maven=3.8.6
              """,
            """
              java=17
              maven=3.8.6
              """,
            sourceSpecs -> sourceSpecs.path(Path.of(".sdkmanrc"))
          )
        );
    }

    @Test
    void noChangeWhenSdkmanrcAlreadyCorrect() {
        rewriteRun(
          mavenProject(
            "test",
            srcMainJava(
              version(
                java(
                  """
                    package com.example;
                    
                    class Service {
                        void doWork() {
                            System.out.println("Working...");
                        }
                    }
                    """
                ), 17)
            )
          ),
          // .sdkmanrc already has correct Java version
          text(
            """
              java=17
              maven=3.9.4
              """,
            sourceSpecs -> sourceSpecs.path(Path.of(".sdkmanrc"))
          )
        );
    }

    @Test
    void noJavaVersionMarkerMeansNoFileCreated() {
        rewriteRun(
          mavenProject(
            "test",
            // Expected .sdkmanrc file should use Java 8 as fallback
            text("""
              non java version text
              """)
          )
        );
    }

    @Test
    void singleJavaVersionProject() {
        rewriteRun(
          mavenProject(
            "test",
            srcMainJava(
              version(
                java(
                  """
                    package com.example;
                    
                    class ModernService {
                        void useNewFeatures() {
                            System.out.println("Using Java 19 features...");
                        }
                    }
                    """
                ), 19)
            )
          ),
          text(
            null,
            """
              java=19
              """,
            sourceSpecs -> sourceSpecs.path(Path.of(".sdkmanrc"))
          )
        );
    }

    @Test
    void multipleSourceSetsHighestVersion() {
        rewriteRun(
          mavenProject(
            "test",
            srcMainJava(
              version(
                java(
                  """
                    package com.example.main;
                    
                    class MainService {
                        void mainWork() {
                            System.out.println("Main work...");
                        }
                    }
                    """
                ), 17)
            ),
            srcTestJava(
              version(
                java(
                  """
                    package com.example.test;
                    
                    class TestHelper {
                        void testWork() {
                            System.out.println("Test work...");
                        }
                    }
                    """
                ), 21)
            )
          ),
          text(
            null,
            """
              java=21
              """,
            sourceSpecs -> sourceSpecs.path(Path.of(".sdkmanrc"))
          )
        );
    }

    @Test
    void addJavaVersionToExistingFile() {
        rewriteRun(
          mavenProject(
            "test",
            srcMainJava(
              version(
                java(
                  """
                    package com.example;
                    
                    class Service {
                        void doWork() {
                            System.out.println("Working...");
                        }
                    }
                    """
                ), 17)
            )
          ),
          // Existing .sdkmanrc without Java version
          text(
            """
              maven=3.9.4
              gradle=7.6
              """,
            """
              maven=3.9.4
              gradle=7.6
              java=17
              """,
            sourceSpecs -> sourceSpecs.path(Path.of(".sdkmanrc"))
          )
        );
    }
}
