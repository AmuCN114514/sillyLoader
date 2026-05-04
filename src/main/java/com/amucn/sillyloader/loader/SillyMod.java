package com.amucn.sillyloader.loader;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SillyMod {
    String name();
    String version() default "1.0.0";
    String description() default "";
    String[] dependencies() default {};
}
