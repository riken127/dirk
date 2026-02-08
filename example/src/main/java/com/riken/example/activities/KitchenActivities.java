package com.riken.example.activities;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface KitchenActivities {
  String makeDough(String type);

  String bake(String dough);
}
