package io.github.riken.dirk;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Functions.Func;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.function.Consumer;
import lombok.extern.log4j.Log4j2;

///
/// Static utilities for use INSIDE Workflow code only. Delegates to Temporal's Workflow class but
/// auto-resolves queues via DirkWorkerRegistry.
///
@Log4j2
public class DirkOps {

  public static <T> T activity(Class<T> activityInterface) {
    return activity(activityInterface, builder -> {});
  }

  public static <T> T activity(
      Class<T> activityInterface, Consumer<ActivityOptions.Builder> configurer) {
    String queue = DirkWorkerRegistry.getQueueFor(activityInterface);

    // DEBUG LOGGING
    if (queue != null) {
      log.info(
          "Resolved queue '{}' for activity '{}'",
          queue,
          activityInterface.getSimpleName());
    } else {
      log.error(
          "Could NOT resolve queue for '{}'. Is it registered?",
          activityInterface.getSimpleName());
    }

    ActivityOptions.Builder builder =
        ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(5)); // Safe default

    if (queue != null) {
      builder.setTaskQueue(queue);
    }

    configurer.accept(builder);

    return Workflow.newActivityStub(activityInterface, builder.build());
  }

  public static <T> T child(Class<T> workflowInterface) {
    return child(workflowInterface, builder -> {});
  }

  public static <T> T child(
      Class<T> workflowInterface, Consumer<ChildWorkflowOptions.Builder> configurer) {
    String queue = DirkWorkerRegistry.getQueueFor(workflowInterface);

    ChildWorkflowOptions.Builder builder = ChildWorkflowOptions.newBuilder();

    if (queue != null) {
      builder.setTaskQueue(queue);
    }

    configurer.accept(builder);

    return Workflow.newChildWorkflowStub(workflowInterface, builder.build());
  }

  public static void sleep(Duration duration) {
    Workflow.sleep(duration);
  }

  public static <T> T sideEffect(Class<T> resultType, Func<T> func) {
    return Workflow.sideEffect(resultType, func);
  }
}
