package com.riken.example;

import com.riken.example.workflows.PizzaWorkflow;

import io.github.riken.dirk.Dirk;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ExampleApplication {

  public static void main(String[] args) {
    SpringApplication.run(ExampleApplication.class, args);
  }

  @Bean
  public CommandLineRunner runDemo(Dirk dirk) {
    return args -> {
      System.out.println("🍕 Starting Pizza Demo via Dirk...");

      String orderId = "order-" + System.currentTimeMillis();

      // Start the workflow!
      PizzaWorkflow workflow = dirk.newStub(PizzaWorkflow.class).setWorkflowId(orderId).build();

      String result = workflow.orderPizza("Pepperoni");
      System.out.println("✅ Workflow Finished: " + result);
      System.exit(0);
    };
  }
}
