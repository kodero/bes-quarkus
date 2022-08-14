package com.corvid.bes.callbacks;

import java.lang.annotation.*;

/**
 * Created by kodero on 5/29/16.
 */

@Inherited
@Target(ElementType.TYPE)
@Repeatable(EntityCallbackClasses.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface EntityCallbackClass {
    Class value();
}
