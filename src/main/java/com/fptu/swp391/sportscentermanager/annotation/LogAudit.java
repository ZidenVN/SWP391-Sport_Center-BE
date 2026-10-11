package com.fptu.swp391.sportscentermanager.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // chi gan duoc cac METHOD
@Retention(RetentionPolicy.RUNTIME)
public @interface LogAudit {
    String actionType(); // CRUD
    String targetEntity(); // entity
}
