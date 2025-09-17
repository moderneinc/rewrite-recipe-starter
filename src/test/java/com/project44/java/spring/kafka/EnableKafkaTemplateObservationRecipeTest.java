/*
 * Moderne Proprietary. Only for use by Moderne customers under the terms of a commercial contract.
 */
package com.project44.java.spring.kafka.kafka;

import com.project44.java.spring.kafka.kafka.EnableKafkaTemplateObservationRecipe;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class EnableKafkaTemplateObservationRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new EnableKafkaTemplateObservationRecipe())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "spring-beans-5", "spring-context-5", "spring-kafka-3"));
    }

    @DocumentExample
    @Test
    void transformsReturnNewKafkaTemplate() {
        rewriteRun(
          //language=java
          java(
            """
              import org.springframework.kafka.core.KafkaTemplate;
              import org.springframework.kafka.core.ProducerFactory;
              import org.springframework.context.annotation.Bean;

              class KafkaConfig {
                  @Bean
                  public KafkaTemplate<Object, Object> confluentAvroKafkaTemplate(final ProducerFactory<Object, Object> confluentAvroProducerFactory) {
                      return new KafkaTemplate<>(confluentAvroProducerFactory);
                  }
              }
              """,
            """
                    import org.springframework.kafka.core.KafkaTemplate;
                    import org.springframework.kafka.core.ProducerFactory;
                    import org.springframework.context.annotation.Bean;

                    class KafkaConfig {
                        @Bean
                        public KafkaTemplate<Object, Object> confluentAvroKafkaTemplate(final ProducerFactory<Object, Object> confluentAvroProducerFactory) {
                            KafkaTemplate<Object, Object> template = new KafkaTemplate<>(confluentAvroProducerFactory);
                            template.setObservationEnabled(true);
                            return template;
                        }
                    }
                    """
          )
        );
    }

    @Test
    void transformsReturnNewKafkaTemplateWithMultipleArguments() {
        rewriteRun(
          //language=java
          java(
            """
              import org.springframework.kafka.core.KafkaTemplate;
              import org.springframework.kafka.core.ProducerFactory;
              import org.springframework.context.annotation.Bean;

              class KafkaConfig {
                  @Bean
                  public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
                      return new KafkaTemplate<>(producerFactory, true);
                  }
              }
              """,
            """
              import org.springframework.kafka.core.KafkaTemplate;
              import org.springframework.kafka.core.ProducerFactory;
              import org.springframework.context.annotation.Bean;

              class KafkaConfig {
                  @Bean
                  public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
                      KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory, true);
                      template.setObservationEnabled(true);
                      return template;
                  }
              }
              """
          )
        );
    }

    @Test
    void addsSetterToExistingVariable() {
        rewriteRun(
          //language=java
          java(
            """
              import org.springframework.kafka.core.KafkaTemplate;
              import org.springframework.kafka.core.ProducerFactory;
              import org.springframework.context.annotation.Bean;

              class KafkaConfig {
                  @Bean
                  public KafkaTemplate<Object, Object> kafkaTemplate(ProducerFactory<Object, Object> producerFactory) {
                      KafkaTemplate<Object, Object> template = new KafkaTemplate<>(producerFactory);
                      return template;
                  }
              }
              """,
            """
              import org.springframework.kafka.core.KafkaTemplate;
              import org.springframework.kafka.core.ProducerFactory;
              import org.springframework.context.annotation.Bean;

              class KafkaConfig {
                  @Bean
                  public KafkaTemplate<Object, Object> kafkaTemplate(ProducerFactory<Object, Object> producerFactory) {
                      KafkaTemplate<Object, Object> template = new KafkaTemplate<>(producerFactory);
                      template.setObservationEnabled(true);
                      return template;
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
              import org.springframework.kafka.core.KafkaTemplate;
              import org.springframework.kafka.core.ProducerFactory;
              import org.springframework.context.annotation.Bean;

              class KafkaConfig {
                  @Bean
                  public KafkaTemplate<Object, Object> kafkaTemplate(ProducerFactory<Object, Object> producerFactory) {
                      KafkaTemplate<Object, Object> template = new KafkaTemplate<>(producerFactory);
                      template.setObservationEnabled(true);
                      return template;
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotModifyNonKafkaTemplateReturnTypes() {
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
    void handlesGenericKafkaTemplate() {
        rewriteRun(
          //language=java
          java(
            """
              import org.springframework.kafka.core.KafkaTemplate;
              import org.springframework.kafka.core.ProducerFactory;
              import org.springframework.context.annotation.Bean;

              class KafkaConfig {
                  @Bean
                  public KafkaTemplate<?, ?> kafkaTemplate(ProducerFactory<?, ?> producerFactory) {
                      return new KafkaTemplate<>(producerFactory);
                  }
              }
              """,
            """
              import org.springframework.kafka.core.KafkaTemplate;
              import org.springframework.kafka.core.ProducerFactory;
              import org.springframework.context.annotation.Bean;

              class KafkaConfig {
                  @Bean
                  public KafkaTemplate<?, ?> kafkaTemplate(ProducerFactory<?, ?> producerFactory) {
                      KafkaTemplate<?, ?> template = new KafkaTemplate<>(producerFactory);
                      template.setObservationEnabled(true);
                      return template;
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
              import org.springframework.kafka.core.KafkaTemplate;
              import org.springframework.kafka.core.ProducerFactory;
              import org.springframework.context.annotation.Bean;

              class KafkaConfig {
                  @Bean
                  public KafkaTemplate<Object, Object> kafkaTemplate(ProducerFactory<Object, Object> producerFactory) {
                      System.out.println("Creating KafkaTemplate");
                      KafkaTemplate<Object, Object> template = new KafkaTemplate<>(producerFactory);
                      template.setDefaultTopic("test-topic");
                      return template;
                  }
              }
              """,
            """
              import org.springframework.kafka.core.KafkaTemplate;
              import org.springframework.kafka.core.ProducerFactory;
              import org.springframework.context.annotation.Bean;

              class KafkaConfig {
                  @Bean
                  public KafkaTemplate<Object, Object> kafkaTemplate(ProducerFactory<Object, Object> producerFactory) {
                      System.out.println("Creating KafkaTemplate");
                      KafkaTemplate<Object, Object> template = new KafkaTemplate<>(producerFactory);
                      template.setDefaultTopic("test-topic");
                      template.setObservationEnabled(true);
                      return template;
                  }
              }
              """
          )
        );
    }

    @Test
    void addsSetterToNonBeanTemplate() {
        rewriteRun(
                //language=java
                java(
                        """
                          import org.springframework.kafka.core.KafkaTemplate;
                          import org.springframework.kafka.core.ProducerFactory;
            
                          class TestUtil {
                              private static KafkaTemplate<Long,String> getTestTemplate(ProducerFactory<Long,String> factory) {
                                  final KafkaTemplate<Long, String> kafkaTemplate = new KafkaTemplate<>(factory); 
                                  kafkaTemplate.setDefaultTopic("test-topic");
                                  return kafkaTemplate;
                              }
                          }
                          """,
                        """
                          import org.springframework.kafka.core.KafkaTemplate;
                          import org.springframework.kafka.core.ProducerFactory;
            
                          class TestUtil {
                              private static KafkaTemplate<Long,String> getTestTemplate(ProducerFactory<Long,String> factory) {
                                  final KafkaTemplate<Long, String> kafkaTemplate = new KafkaTemplate<>(factory); 
                                  kafkaTemplate.setDefaultTopic("test-topic");
                                  kafkaTemplate.setObservationEnabled(true);
                                  return kafkaTemplate;
                              }
                          }
                          """
                )
        );
    }


}
