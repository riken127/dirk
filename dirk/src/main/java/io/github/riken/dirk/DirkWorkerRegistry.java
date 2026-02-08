package io.github.riken.dirk;

import io.temporal.activity.ActivityInterface;
import io.temporal.client.WorkflowClient;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.WorkflowInterface;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.util.ClassUtils;

@Slf4j
public class DirkWorkerRegistry implements SmartLifecycle, ApplicationContextAware {
  private final WorkflowClient workflowClient;
  private final DirkProperties properties;
  private WorkerFactory workerFactory;
  private ApplicationContext applicationContext;
  private boolean isRunning = false;

  private final Map<Class<?>, String> workflowQueueMap = new ConcurrentHashMap<>();
  private final Map<Class<?>, String> activityQueueMap = new ConcurrentHashMap<>();

  private static DirkWorkerRegistry INSTANCE;

  public DirkWorkerRegistry(WorkflowClient workflowClient, DirkProperties properties) {
    this.workflowClient = workflowClient;
    this.properties = properties;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  public void start() {
    INSTANCE = this;
    log.info("Starting dirk worker registry...");
    this.workerFactory = WorkerFactory.newInstance(workflowClient);

    registerManualQueues();

    if (properties.isAutoPilot() || properties.isAutoNaming()) {
      registerAutoDiscoveredWorkflows();
    }

    this.workerFactory.start();
    this.isRunning = true;
    log.info("Dirk workers started successfully.");
  }

  public static String getQueueFor(Class<?> clazz) {
    if (INSTANCE == null) return null;
    return INSTANCE.workflowQueueMap.getOrDefault(clazz, INSTANCE.activityQueueMap.get(clazz));
  }

  private void registerManualQueues() {
    if (properties.getQueues() == null) return;

    for (var config : properties.getQueues()) {
      var worker = workerFactory.newWorker(config.getName());

      for (var workflowClass : config.getWorkflows()) {
        worker.registerWorkflowImplementationTypes(workflowClass);
        log.info(
            "Registered workflow {} on queue {}", workflowClass.getSimpleName(), config.getName());
      }

      for (var activityClass : config.getActivities()) {
        var activityBean = applicationContext.getBean(activityClass);
        worker.registerActivitiesImplementations(activityBean);
        log.info(
            "Registered activity {} on queue {}", activityClass.getSimpleName(), config.getName());
      }
    }
  }

  private void registerAutoDiscoveredWorkflows() {
    log.debug("Scanning package '{}' for temporal workflows and activities...");

    // Find all classes annotated with @WorkflowInterface (Interfaces)
    // Note: We actually need to find the IMPLEMENTATIONS, not the interfaces.
    // Usually, the implementation implements the interface.
    // A better strategy for Spring: Find all Beans that implement a WorkflowInterface.

    String[] beanNames = applicationContext.getBeanDefinitionNames();
    for (String beanName : beanNames) {
      Object bean = applicationContext.getBean(beanName);
      Class<?> beanClass = ClassUtils.getUserClass(bean);

      // Check if this bean implements a @WorkflowInterface
      if (isWorkflowImplementation(beanClass)) {
        String queueName = determineQueueName(beanClass);
        workflowQueueMap.put(beanClass, queueName);
        Worker worker = getOrCreateWorker(queueName);
        worker.registerWorkflowImplementationTypes(beanClass);
        log.info("Auto-registered workflow {} on queue {}", beanClass.getSimpleName(), queueName);
      }

      // Check if this bean implements an @ActivityInterface
      if (properties.isAutoPilot() && isActivityImplementation(beanClass)) {
        // For Activities, we usually want to register them on the SAME queue as the workflow
        // that calls them. This is tricky in auto-pilot.
        // Strategy: Put them on the Default Queue or use an annotation @DirkActivity(queue="...")
        // For now, let's put them on the Default Queue or imply logic.

        String queueName = properties.getDefaultQueue();
        Worker worker = getOrCreateWorker(queueName);
        activityQueueMap.put(beanClass, queueName);

        for (var iface : beanClass.getInterfaces()) {
          if (iface.isAnnotationPresent(ActivityInterface.class)) {
            activityQueueMap.put(iface, queueName);
          }
        }

        worker.registerActivitiesImplementations(bean);
        log.info("Auto-registered activity {} on queue {}", beanClass.getSimpleName(), queueName);
      }
    }
  }

  // Helper to check for @WorkflowInterface on interfaces
  private boolean isWorkflowImplementation(Class<?> cls) {
    for (Class<?> iface : cls.getInterfaces()) {
      if (iface.isAnnotationPresent(WorkflowInterface.class)) return true;
    }
    return false;
  }

  // Helper to check for @ActivityInterface on interfaces
  private boolean isActivityImplementation(Class<?> cls) {
    for (Class<?> iface : cls.getInterfaces()) {
      if (iface.isAnnotationPresent(ActivityInterface.class)) return true;
    }
    return false;
  }

  private String determineQueueName(Class<?> cls) {
    if (properties.isAutoNaming()) {
      // If it's a Workflow Implementation, use the INTERFACE name
      // (This aligns with what the Client expects: PizzaWorkflow -> PIZZA_WORKFLOW_QUEUE)
      for (Class<?> iface : cls.getInterfaces()) {
        if (iface.isAnnotationPresent(WorkflowInterface.class)) {
          return toSnakeCase(iface.getSimpleName()).toUpperCase() + "_QUEUE";
        }
      }

      // Fallback for Activities or classes without annotated interfaces
      // (KitchenActivitiesImpl -> KITCHEN_ACTIVITIES_IMPL_QUEUE)
      return toSnakeCase(cls.getSimpleName()).toUpperCase() + "_QUEUE";
    }
    return properties.getDefaultQueue();
  }

  private Worker getOrCreateWorker(String queueName) {
    try {
      return workerFactory.getWorker(queueName);
    } catch (IllegalArgumentException e) {
      return workerFactory.newWorker(queueName);
    }
  }

  private String toSnakeCase(String str) {
    return str.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
  }

  @Override
  public void stop() {
    if (workerFactory != null) {
      workerFactory.shutdown();
    }
    this.isRunning = false;
  }

  @Override
  public boolean isRunning() {
    return isRunning;
  }
}
