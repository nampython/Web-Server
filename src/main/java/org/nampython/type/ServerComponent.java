package org.nampython.type;


import org.ioc.stereotype.AliasFor;
import org.ioc.stereotype.Service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@AliasFor(Service.class)
public @interface ServerComponent {
}
