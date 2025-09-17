/*
 * Moderne Proprietary. Only for use by Moderne customers under the terms of a commercial contract.
 */
package com.project44.java.spring.kafka.kafka;

import com.project44.java.spring.kafka.kafka.EnableKafkaListenerContainerFactoryObservationRecipe;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class EnableKafkaListenerContainerFactoryObservationRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new EnableKafkaListenerContainerFactoryObservationRecipe())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(),
                  "spring-beans-5", "spring-context-5", "spring-kafka-3"));
    }

    @DocumentExample
    @Test
    void addsObservationEnabledToExistingFactory() {
        rewriteRun(
          //language=java
          java(
            """
              import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
              import org.springframework.kafka.core.ConsumerFactory;
              import org.springframework.context.annotation.Bean;

              class KafkaConfig {
                  @Bean
                  public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(final ConsumerFactory<String, String> consumerFactory) {
                    ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
                    factory.setConsumerFactory(consumerFactory);
                    factory.setConcurrency(3);
                    return factory;
                  }
              }
              """,
            """
              import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
              import org.springframework.kafka.core.ConsumerFactory;
              import org.springframework.context.annotation.Bean;

              class KafkaConfig {
                  @Bean
                  public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(final ConsumerFactory<String, String> consumerFactory) {
                    ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
                    factory.setConsumerFactory(consumerFactory);
                    factory.setConcurrency(3);
                    factory.getContainerProperties().setObservationEnabled(true);
                    return factory;
                  }
              }
              """
          )
        );
    }

    @Test
    void addsObservationEnabledWithSimpleFactory() {
        rewriteRun(
          //language=java
          java(
            """
              import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
              import org.springframework.kafka.core.ConsumerFactory;
              import org.springframework.context.annotation.Bean;

              class KafkaConfig {
                  @Bean
                  public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(ConsumerFactory<String, String> consumerFactory) {
                    ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
                    factory.setConsumerFactory(consumerFactory);
                    return factory;
                  }
              }
              """,
            """
              import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
              import org.springframework.kafka.core.ConsumerFactory;
              import org.springframework.context.annotation.Bean;

              class KafkaConfig {
                  @Bean
                  public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(ConsumerFactory<String, String> consumerFactory) {
                    ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
                    factory.setConsumerFactory(consumerFactory);
                    factory.getContainerProperties().setObservationEnabled(true);
                    return factory;
                  }
              }
              """
          )
        );
    }

    @Test
    void handlesExistingObservationEnabledSetter() {
        rewriteRun(
                //language=java
                java(
                        """
                          import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
                          import org.springframework.kafka.core.ConsumerFactory;
                          import org.springframework.context.annotation.Bean;
            
                          class KafkaConfig {
                              @Bean
                              public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(ConsumerFactory<Object, Object> consumerFactory) {
                                ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
                                factory.setConsumerFactory(consumerFactory);
                                factory.getContainerProperties().setObservationEnabled(true);
                                return factory;
                              }
                          }
                          """
                )
        );
    }

    @Test
    void addsSetterToNonBean() {
        rewriteRun(
                //language=java
                java(
                        """
                          import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
                          import org.springframework.kafka.core.ConsumerFactory;
                          import org.springframework.context.annotation.Bean;
                          
                          class TestUtil {
                              static public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(ConsumerFactory<String, String> consumerFactory) {
                                ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
                                factory.setConsumerFactory(consumerFactory);
                                return factory;
                              }
                          }
                          """,
                        """
                          import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
                          import org.springframework.kafka.core.ConsumerFactory;
                          import org.springframework.context.annotation.Bean;
                          
                          class TestUtil {
                              static public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(ConsumerFactory<String, String> consumerFactory) {
                                ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
                                factory.setConsumerFactory(consumerFactory);
                                factory.getContainerProperties().setObservationEnabled(true);
                                return factory;
                              }
                          }
                          """
                )
        );
    }

    @Test
    void doesNotModifyNonListenerContainerFactoryReturnTypes() {
        rewriteRun(
          //language=java
          java(
            """
              import org.springframework.context.annotation.Bean;

              class SomeConfig {
                  @Bean
                  public String someBean() {
                      return "test";
                  }
              }
              """
          )
        );
    }

    @Test
    void handlesGenericListenerContainerFactory() {
        rewriteRun(
          //language=java
          java(
            """
              import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
              import org.springframework.kafka.core.ConsumerFactory;
              import org.springframework.context.annotation.Bean;

              class KafkaConfig {
                  @Bean
                  public ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(ConsumerFactory<?, ?> consumerFactory) {
                    ConcurrentKafkaListenerContainerFactory<?, ?> factory = new ConcurrentKafkaListenerContainerFactory<>();
                    return factory;
                  }
              }
              """,
            """
              import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
              import org.springframework.kafka.core.ConsumerFactory;
              import org.springframework.context.annotation.Bean;

              class KafkaConfig {
                  @Bean
                  public ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(ConsumerFactory<?, ?> consumerFactory) {
                    ConcurrentKafkaListenerContainerFactory<?, ?> factory = new ConcurrentKafkaListenerContainerFactory<>();
                    factory.getContainerProperties().setObservationEnabled(true);
                    return factory;
                  }
              }
              """
          )
        );
    }

    @Test
    void handlesMultipleStatements() {
        rewriteRun(
          //language=java
          java(
            """
              import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
              import org.springframework.kafka.core.ConsumerFactory;
              import org.springframework.context.annotation.Bean;

              class KafkaConfig {
                  @Bean
                  public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(ConsumerFactory<Object, Object> consumerFactory) {
                    System.out.println("Creating listener container factory");
                    ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
                    factory.setConsumerFactory(consumerFactory);
                    factory.setConcurrency(3);
                    factory.setBatchListener(true);
                    return factory;
                  }
              }
              """,
            """
              import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
              import org.springframework.kafka.core.ConsumerFactory;
              import org.springframework.context.annotation.Bean;

              class KafkaConfig {
                  @Bean
                  public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(ConsumerFactory<Object, Object> consumerFactory) {
                    System.out.println("Creating listener container factory");
                    ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
                    factory.setConsumerFactory(consumerFactory);
                    factory.setConcurrency(3);
                    factory.setBatchListener(true);
                    factory.getContainerProperties().setObservationEnabled(true);
                    return factory;
                  }
              }
              """
          )
        );
    }
}
