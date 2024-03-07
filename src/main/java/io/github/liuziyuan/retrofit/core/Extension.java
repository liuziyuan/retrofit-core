package io.github.liuziyuan.retrofit.core;

import java.lang.annotation.Annotation;

public interface Extension {

    Class<? extends Annotation> createAnnotation();

    Class<?> createInterceptor();
}
