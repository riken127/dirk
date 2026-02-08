package com.riken.example.activities;

import org.springframework.stereotype.Component;

@Component // Dirk scans this too!
public class KitchenActivitiesImpl implements KitchenActivities {
  @Override
  public String makeDough(String type) {
    return "Dough(" + type + ")";
  }

  @Override
  public String bake(String dough) {
    return "Baked " + dough;
  }
}
