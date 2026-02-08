package com.riken.example.workflows;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface PizzaWorkflow {
  @WorkflowMethod
  String orderPizza(String type);
}
