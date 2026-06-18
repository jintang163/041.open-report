package com.openreport.admin.config;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePerms {

    String[] value();

    Logical logical() default Logical.AND;
}
