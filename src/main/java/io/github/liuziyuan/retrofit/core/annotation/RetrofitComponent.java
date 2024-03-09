package io.github.liuziyuan.retrofit.core.annotation;

import java.lang.annotation.*;

/**
 * 需要注入的Retrofit组件，可以用这个声明
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RetrofitComponent {
}
