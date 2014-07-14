package at.kokutoru.tool.ui;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by luca on 01.07.14.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface UI_ELEMENT {
    public String name() default "";
    public String onclick() default "";
}
