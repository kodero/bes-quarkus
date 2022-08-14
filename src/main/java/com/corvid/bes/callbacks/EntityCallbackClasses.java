package com.corvid.bes.callbacks;

import java.lang.annotation.*;

/**
 * Created by kodero on 7/20/16.
 */
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EntityCallbackClasses {
    EntityCallbackClass[] value();
}
