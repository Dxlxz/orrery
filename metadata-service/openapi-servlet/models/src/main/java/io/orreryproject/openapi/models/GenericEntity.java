package io.orreryproject.openapi.models;

import java.util.Map;

public interface GenericEntity<T> {
  Map<String, T> getAspects();
}
