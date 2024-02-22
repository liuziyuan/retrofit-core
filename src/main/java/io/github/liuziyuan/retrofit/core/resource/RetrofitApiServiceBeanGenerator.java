package io.github.liuziyuan.retrofit.core.resource;

import io.github.liuziyuan.retrofit.core.Env;
import io.github.liuziyuan.retrofit.core.Extension;
import io.github.liuziyuan.retrofit.core.annotation.*;
import io.github.liuziyuan.retrofit.core.exception.RetrofitStarterException;
import io.github.liuziyuan.retrofit.core.generator.Generator;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * generate RetrofitServiceBean object
 *
 * @author liuziyuan
 */
public class RetrofitApiServiceBeanGenerator implements Generator<RetrofitApiServiceBean> {
    private final Class<?> clazz;
    private final Env env;

    public RetrofitApiServiceBeanGenerator(Class<?> clazz, Env env) {
        this.clazz = clazz;
        this.env = env;
    }

    public RetrofitApiServiceBean generate(List<Extension> extensions) {
        return generateInner(extensions);
    }

    private RetrofitApiServiceBean generateInner(@Nullable List<Extension> extensions) {
        Class<?> retrofitBuilderClazz = getParentRetrofitBuilderClazz();
        RetrofitApiServiceBean retrofitApiServiceBean = new RetrofitApiServiceBean();
        retrofitApiServiceBean.setSelfClazz(clazz);
        retrofitApiServiceBean.setParentClazz(retrofitBuilderClazz);
        RetrofitBuilder retrofitBuilderAnnotation = retrofitBuilderClazz.getDeclaredAnnotation(RetrofitBuilder.class);
        retrofitApiServiceBean.setRetrofitBuilder(retrofitBuilderAnnotation);
        Set<RetrofitInterceptor> interceptors = getInterceptors(retrofitBuilderClazz);
        Set<RetrofitInterceptor> myInterceptors = getInterceptors(clazz);
        if (extensions != null) {
            for (Extension extension : extensions) {
                try {
                    RetrofitInterceptor annotation = extension.createAnnotation().getAnnotation(RetrofitInterceptor.class);
                    myInterceptors.add(annotation);
                } catch (NullPointerException ignored) {
                }
            }
        }
        retrofitApiServiceBean.setMyInterceptors(myInterceptors);
        retrofitApiServiceBean.setInterceptors(interceptors);
        RetrofitUrl retrofitUrl = getRetrofitUrl(retrofitBuilderAnnotation);
        retrofitApiServiceBean.setRetrofitUrl(retrofitUrl);
        return retrofitApiServiceBean;
    }

    @Override
    public RetrofitApiServiceBean generate() {
        return generateInner(null);

    }

    private RetrofitUrl getRetrofitUrl(RetrofitBuilder retrofitBuilderAnnotation) {
        final RetrofitUrlPrefix retrofitUrlPrefix = clazz.getDeclaredAnnotation(RetrofitUrlPrefix.class);
        final RetrofitDynamicBaseUrl retrofitDynamicBaseUrl = clazz.getDeclaredAnnotation(RetrofitDynamicBaseUrl.class);
        String retrofitDynamicBaseUrlValue = retrofitDynamicBaseUrl == null ? null : retrofitDynamicBaseUrl.value();
        return new RetrofitUrl(retrofitBuilderAnnotation.baseUrl(),
                retrofitDynamicBaseUrlValue,
                retrofitUrlPrefix == null ? null : retrofitUrlPrefix.value(),
                env);
    }

    private Class<?> getParentRetrofitBuilderClazz() {
        return findParentClazzIncludeRetrofitBuilderAndBase(clazz);
    }

    private Class<?> findParentClazzIncludeRetrofitBuilderAndBase(Class<?> clazz) {
        Class<?> retrofitBuilderClazz;
        if (clazz.getDeclaredAnnotation(RetrofitBase.class) != null) {
            retrofitBuilderClazz = findParentRetrofitBaseClazz(clazz);
        } else {
            retrofitBuilderClazz = findParentRetrofitBuilderClazz(clazz);
        }
        if (retrofitBuilderClazz.getDeclaredAnnotation(RetrofitBuilder.class) == null) {
            retrofitBuilderClazz = findParentClazzIncludeRetrofitBuilderAndBase(retrofitBuilderClazz);
        }
        return retrofitBuilderClazz;
    }

    private Class<?> findParentRetrofitBuilderClazz(Class<?> clazz) {
        RetrofitBuilder retrofitBuilder = clazz.getDeclaredAnnotation(RetrofitBuilder.class);
        Class<?> targetClazz = clazz;
        if (retrofitBuilder == null) {
            Class<?>[] interfaces = clazz.getInterfaces();
            if (interfaces.length > 0) {
                targetClazz = findParentRetrofitBuilderClazz(interfaces[0]);
            } else {
                if (clazz.getDeclaredAnnotation(RetrofitBase.class) == null) {
                    throw new RetrofitStarterException("The baseApi of @RetrofitBase in the [" + clazz.getSimpleName() + "] Interface, does not define @RetrofitBuilder");
                }
            }
        }
        return targetClazz;
    }

    private Class<?> findParentRetrofitBaseClazz(Class<?> clazz) {
        RetrofitBase retrofitBase = clazz.getDeclaredAnnotation(RetrofitBase.class);
        Class<?> targetClazz = clazz;
        if (retrofitBase != null) {
            final Class<?> baseApiClazz = retrofitBase.baseInterface();
            if (baseApiClazz != null) {
                targetClazz = findParentRetrofitBaseClazz(baseApiClazz);
            }
        }
        return targetClazz;
    }

    private Set<RetrofitInterceptor> getInterceptors(Class<?> clazz) {
        Annotation[] annotations = clazz.getDeclaredAnnotations();
        Set<RetrofitInterceptor> retrofitInterceptorAnnotations = new LinkedHashSet<>();
        for (Annotation annotation : annotations) {
            if (annotation instanceof Interceptors) {
                RetrofitInterceptor[] values = ((Interceptors) annotation).value();
                Collections.addAll(retrofitInterceptorAnnotations, values);
            } else if (annotation instanceof RetrofitInterceptor) {
                retrofitInterceptorAnnotations.add((RetrofitInterceptor) annotation);
            }
        }
        return retrofitInterceptorAnnotations;
    }


}
