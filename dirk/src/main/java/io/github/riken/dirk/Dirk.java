package io.github.riken.dirk;

import io.temporal.client.ActivityCompletionClient;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.common.RetryOptions;
import io.temporal.common.SearchAttributes;
import io.temporal.common.context.ContextPropagator;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/// The main entry point for interacting with Temporal workflows via Dirk
@Log4j2
@RequiredArgsConstructor
public class Dirk {
  private final WorkflowClient client;
  private final DirkWorkerRegistry registry;
  private final DirkProperties properties;

  /**
   * Start building a new Workflow Stub. Use this to start a new workflow execution.
   *
   * @param workflowInterface The interface annotated with @WorkflowInterface
   * @param <T> The workflow type
   * @return A builder to configure execution options
   */
  public <T> DirkWorkflowBuilder<T> newStub(Class<T> workflowInterface) {
    return new DirkWorkflowBuilder<>(workflowInterface, client, registry, properties);
  }

  /**
   * Get a stub for an existing workflow execution (e.g., to signal or query it).
   *
   * @param workflowInterface The interface annotated with @WorkflowInterface
   * @param workflowId The business ID of the running workflow
   * @param <T> The workflow type
   * @return The workflow stub
   */
  public <T> T existingStub(Class<T> workflowInterface, String workflowId) {
    return client.newWorkflowStub(workflowInterface, workflowId);
  }

  public WorkflowStub newUntypedStub(String workflowType, WorkflowOptions options) {
    return client.newUntypedWorkflowStub(workflowType, options);
  }

  public WorkflowStub existingUntypedStub(String workflowId) {
    return client.newUntypedWorkflowStub(workflowId);
  }

  public <T> ActivityCompletionClient completionClient() {
    return client.newActivityCompletionClient();
  }

  public static class DirkWorkflowBuilder<T> {
    private final Class<T> workflowInterface;
    private final WorkflowClient client;
    private final DirkProperties properties;

    // Delegate directly to the official Builder to avoid missing fields.
    private final WorkflowOptions.Builder optionsBuilder;

    public DirkWorkflowBuilder(
        Class<T> workflowInterface,
        WorkflowClient client,
        DirkWorkerRegistry registry,
        DirkProperties properties) {
      this.workflowInterface = workflowInterface;
      this.client = client;
      this.properties = properties;
      this.optionsBuilder = WorkflowOptions.newBuilder();

      String resolvedQueue = registry.getQueueFor(workflowInterface);
      if (resolvedQueue == null) {
        if (properties.isAutoNaming()) {
          resolvedQueue = toSnakeCase(workflowInterface.getSimpleName()).toUpperCase() + "_QUEUE";
        } else {
          resolvedQueue = properties.getDefaultQueue();
        }
      }

      this.optionsBuilder.setTaskQueue(resolvedQueue);
    }

    public DirkWorkflowBuilder<T> setWorkflowId(String workflowId) {
      optionsBuilder.setWorkflowId(workflowId);
      return this;
    }

    public DirkWorkflowBuilder<T> setTaskQueue(String taskQueue) {
      optionsBuilder.setTaskQueue(taskQueue);
      return this;
    }

    public DirkWorkflowBuilder<T> setWorkflowRunTimeout(Duration timeout) {
      optionsBuilder.setWorkflowRunTimeout(timeout);
      return this;
    }

    public DirkWorkflowBuilder<T> setWorkflowExecutionTimeout(Duration timeout) {
      optionsBuilder.setWorkflowExecutionTimeout(timeout);
      return this;
    }

    public DirkWorkflowBuilder<T> setWorkflowTaskTimeout(Duration timeout) {
      optionsBuilder.setWorkflowTaskTimeout(timeout);
      return this;
    }

    public DirkWorkflowBuilder<T> setRetryOptions(RetryOptions retryOptions) {
      optionsBuilder.setRetryOptions(retryOptions);
      return this;
    }

    public DirkWorkflowBuilder<T> setCronSchedule(String cronSchedule) {
      optionsBuilder.setCronSchedule(cronSchedule);
      return this;
    }

    public DirkWorkflowBuilder<T> setMemo(Map<String, Object> memo) {
      optionsBuilder.setMemo(memo);
      return this;
    }

    public DirkWorkflowBuilder<T> setSearchAttributes(SearchAttributes searchAttributes) {
      optionsBuilder.setTypedSearchAttributes(searchAttributes);
      return this;
    }

    public DirkWorkflowBuilder<T> setContextPropagators(List<ContextPropagator> propagators) {
      optionsBuilder.setContextPropagators(propagators);
      return this;
    }

    public DirkWorkflowBuilder<T> validateBuildWithDefaults() {
      optionsBuilder.validateBuildWithDefaults();
      return this;
    }

    /**
     * Finalizes the stub. If no TaskQueue is set, Dirk attempts to auto-resolve it based on the
     * properties and class name.
     */
    public T build() {
      return client.newWorkflowStub(workflowInterface, optionsBuilder.build());
    }

    private String toSnakeCase(String str) {
      return str.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }
  }
}
