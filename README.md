# Dirk 🗡️

**Opinionated Temporal.io integration for Spring Boot.**

This project began as a personal draft and was uploaded to GitHub mainly for reference and preservation.

The official SDK now provides built-in auto-discovery for Activities and Workflows, rendering this implementation obsolete.

**Not recommended for production use. This project is archived and may be discontinued.**

## 🚀 Features

* **Zero Config:** Auto-discovery of `@WorkflowInterface` and `@ActivityInterface` beans.
* **Auto-Naming:** Queues are automatically named based on your interface names (e.g., `PizzaWorkflow` -> `PIZZA_WORKFLOW_QUEUE`).
* **Smart Stubs:** `Dirk.newStub(MyWorkflow.class)` automatically resolves the correct queue.
* **Spring Native:** Works with your existing Spring beans and dependency injection.

## 📦 Installation

Add this to your `pom.xml` via JitPack (currently work in progress):

```xml
<dependencies>
    <dependency>
        <groupId>com.github.riken</groupId> <artifactId>dirk</artifactId>
        <version>main-SNAPSHOT</version> </dependency>
</dependencies>

```

## ⚡ Quick Start

### 1. Enable Dirk

Dirk auto-configures itself. Just make sure your `application.yml` points to your Temporal server:

```yaml
dirk:
  enabled: true
  auto-naming: true
  target: 127.0.0.1:7233

```

### 2. Write a Workflow

Just use standard Temporal annotations. Dirk will find them.

```java
@WorkflowInterface
public interface PizzaWorkflow {
    @WorkflowMethod
    String orderPizza(String type);
}

@Component // Dirk scans Spring beans!
public class PizzaWorkflowImpl implements PizzaWorkflow {
    
    // Use DirkOps to safely call activities inside workflows
    private final KitchenActivities kitchen = DirkOps.activity(KitchenActivities.class);

    @Override
    public String orderPizza(String type) {
        return kitchen.bake(type);
    }
}

```

### 3. Start a Workflow

Inject `Dirk` into your Controller or Service:

```java
@RestController
@RequiredArgsConstructor
public class PizzaController {

    private final Dirk dirk;

    @PostMapping("/order")
    public String order(@RequestParam String type) {
        // Dirk automatically finds the queue for PizzaWorkflow!
        PizzaWorkflow workflow = dirk.newStub(PizzaWorkflow.class)
            .setWorkflowId("order-" + System.currentTimeMillis())
            .build();

        // Asynchronous start
        WorkflowExecution execution = WorkflowClient.start(workflow::orderPizza, type);
        return execution.getWorkflowId();
    }
}

```

## 🏗️ Building Locally

```bash
cd dirk
mvn clean install

```

## 📄 License

MIT
