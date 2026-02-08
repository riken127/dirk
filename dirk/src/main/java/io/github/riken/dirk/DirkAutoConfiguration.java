package io.github.riken.dirk;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DirkProperties.class)
@ConditionalOnProperty(
    prefix = "dirk",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class DirkAutoConfiguration {
  @Bean
  public WorkflowServiceStubs workflowServiceStubs(DirkProperties props) {
    // TODO: Add logic to use props.getTarget()
    return WorkflowServiceStubs.newLocalServiceStubs();
  }

  @Bean
  public WorkflowClient workflowClient(WorkflowServiceStubs service) {
    return WorkflowClient.newInstance(service);
  }

  @Bean
  public Dirk dirk(WorkflowClient client, DirkWorkerRegistry registry, DirkProperties props) {
    return new Dirk(client, registry, props);
  }

  @Bean
  public DirkWorkerRegistry dirkWorkerRegistry(WorkflowClient client, DirkProperties props) {
    return new DirkWorkerRegistry(client, props);
  }
}
