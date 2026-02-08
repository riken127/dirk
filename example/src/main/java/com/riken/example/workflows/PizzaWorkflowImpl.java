package com.riken.example.workflows;

import com.riken.example.activities.KitchenActivities;

import io.github.riken.dirk.DirkOps;

import org.springframework.stereotype.Component;

@Component // Dirk scans this!
public class PizzaWorkflowImpl implements PizzaWorkflow {

  @Override
  public String orderPizza(String type) {
    var kitchen = DirkOps.activity(KitchenActivities.class);

    String dough = kitchen.makeDough(type);
    return kitchen.bake(dough);
  }
}
