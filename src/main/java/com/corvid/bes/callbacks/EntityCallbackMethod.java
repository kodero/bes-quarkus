package com.corvid.bes.callbacks;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by kodero on 5/29/16.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD) //can use in method only.
public @interface EntityCallbackMethod {
    int priority() default 0;
    boolean enabled() default true;
    During[] when() default During.ALWAYS;
}
