package com.clearspend.capital.permissioncheck.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/** Annotation for service methods that are allowed to be called from anywhere. */
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
public @interface OpenAccessAPI {

  String explanation();

  String reviewer();
}
