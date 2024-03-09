package io.github.liuziyuan.retrofit.core;

import io.github.liuziyuan.retrofit.core.extension.BaseInterceptor;

import java.lang.annotation.Annotation;

public interface RetrofitInterceptorExtension {

    Class<? extends Annotation> createAnnotation();

    Class<? extends BaseInterceptor> createInterceptor();
}
