/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yourorg;

import com.yourorg.table.SensitiveDataInLogsReport;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class FindSensitiveDataInLogsTest implements RewriteTest {

    @Language("java")
    private static final String ACCOUNT = """
      package com.yourorg;

      class Account {
          private final String username;
          private final String password;

          Account(String username, String password) {
              this.username = username;
              this.password = password;
          }

          String getUsername() {
              return username;
          }

          String getPassword() {
              return password;
          }
      }
      """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindSensitiveDataInLogs("com.yourorg.Account getPassword()", null))
          .parser(JavaParser.fromJavaVersion().classpath("slf4j-api"));
    }

    @DocumentExample
    @Test
    void sensitiveValueReachesLogThroughLocalVariableAndConcatenation() {
        rewriteRun(
          spec -> spec.dataTable(SensitiveDataInLogsReport.Row.class, rows ->
            assertThat(rows).singleElement().satisfies(row -> {
                assertThat(row.getSource()).isEqualTo("account.getPassword()");
                assertThat(row.getSink()).isEqualTo("message");
            })),
          java(ACCOUNT),
          java(
            """
              package com.yourorg;

              import org.slf4j.Logger;
              import org.slf4j.LoggerFactory;

              class LoginService {
                  private static final Logger logger = LoggerFactory.getLogger(LoginService.class);

                  void login(Account account) {
                      String password = account.getPassword();
                      String message = "Login failed for " + password;
                      logger.info(message);
                  }
              }
              """,
            """
              package com.yourorg;

              import org.slf4j.Logger;
              import org.slf4j.LoggerFactory;

              class LoginService {
                  private static final Logger logger = LoggerFactory.getLogger(LoginService.class);

                  void login(Account account) {
                      String password = /*~~>*/account.getPassword();
                      String message = "Login failed for " + password;
                      logger.info(message);
                  }
              }
              """
          )
        );
    }

    @Test
    void sensitiveValueLoggedDirectly() {
        rewriteRun(
          java(ACCOUNT),
          java(
            """
              package com.yourorg;

              import org.slf4j.Logger;
              import org.slf4j.LoggerFactory;

              class LoginService {
                  private static final Logger logger = LoggerFactory.getLogger(LoginService.class);

                  void login(Account account) {
                      logger.debug(account.getPassword());
                  }
              }
              """,
            """
              package com.yourorg;

              import org.slf4j.Logger;
              import org.slf4j.LoggerFactory;

              class LoginService {
                  private static final Logger logger = LoggerFactory.getLogger(LoginService.class);

                  void login(Account account) {
                      logger.debug(/*~~>*/account.getPassword());
                  }
              }
              """
          )
        );
    }

    @Test
    void nonSensitiveValueIsNotReported() {
        rewriteRun(
          java(ACCOUNT),
          java(
            """
              package com.yourorg;

              import org.slf4j.Logger;
              import org.slf4j.LoggerFactory;

              class LoginService {
                  private static final Logger logger = LoggerFactory.getLogger(LoginService.class);

                  void login(Account account) {
                      String user = account.getUsername();
                      logger.info("Login failed for " + user);
                  }
              }
              """
          )
        );
    }

    @Test
    void sensitiveValueThatNeverReachesALogIsNotReported() {
        rewriteRun(
          java(ACCOUNT),
          java(
            """
              package com.yourorg;

              import org.slf4j.Logger;
              import org.slf4j.LoggerFactory;

              class LoginService {
                  private static final Logger logger = LoggerFactory.getLogger(LoginService.class);

                  boolean login(Account account, String attempt) {
                      String password = account.getPassword();
                      logger.info("Login attempt for " + account.getUsername());
                      return password.equals(attempt);
                  }
              }
              """
          )
        );
    }
}
