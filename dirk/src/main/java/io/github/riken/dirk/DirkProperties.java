package io.github.riken.dirk;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "dirk")
public class DirkProperties {
  /// Enable or disable dirk.
  private boolean enabled = true;

  /// The target Temporal Service address (e.g., "127.0.0.1:7233").
  private String target = "127.0.0.1:7233";

  /// The Temporal Namespace to connect to.
  private String namespace;

  /// Default Task Queue for workflows not explictly configured.
  private String defaultQueue = "DEFAULT_WORKER_QUEUE";

  /// Automatically generate queue names from workflow class names.
  /// (e.g., OrderWorkflow -> ORDER_WORKFLOW_QUEUE).
  private boolean autoNaming = true;

  /// Automatically register all activities associated with a workflow found in the same
  // package/scan scope.
  private boolean autoPilot = true;

  /// Base package to scan for Workflow and Activities.
  /// If null, defaults to the main application class package.
  private String scanBasePackage;

  /// Manual configuration for specific queues.
  private List<QueueConfig> queues = new ArrayList<>();

  @Data
  public static class QueueConfig {
    private String name;
    private List<Class<?>> workflows = new ArrayList<>();
    private List<Class<?>> activities = new ArrayList<>();
  }
}
