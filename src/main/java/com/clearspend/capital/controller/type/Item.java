package com.clearspend.capital.controller.type;

import lombok.Value;

/**
 * A generic item with an id and name to display on UI
 *
 * @param <T> Id type
 */
@Value
public class Item<T> {

  private T id;

  private String name;
}
