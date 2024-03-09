package io.github.liuziyuan.retrofit.core.resource;

import io.github.liuziyuan.retrofit.core.Env;
import io.github.liuziyuan.retrofit.core.RetrofitInterceptorExtension;
import io.github.liuziyuan.retrofit.core.OverrideRule;
import io.github.liuziyuan.retrofit.core.annotation.*;
import io.github.liuziyuan.retrofit.core.builder.BaseCallBackExecutorBuilder;
import io.github.liuziyuan.retrofit.core.builder.BaseCallFactoryBuilder;
import io.github.liuziyuan.retrofit.core.builder.BaseOkHttpClientBuilder;
import io.github.liuziyuan.retrofit.core.exception.RetrofitStarterException;
import io.github.liuziyuan.retrofit.core.generator.Generator;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * generate RetrofitServiceBean object
 *
 * @author liuziyuan
 */
public class RetrofitApiServiceBeanGenerator implements Generator<RetrofitApiServiceBean> {
    private final Class<?> clazz;
    private final Env env;
    private final RetrofitBuilderBean globalRetrofitBuilderBean;
    private final List<RetrofitInterceptorExtension> interceptorExtensions;

    public RetrofitApiServiceBeanGenerator(Class<?> clazz, Env env, RetrofitBuilderBean globalRetrofitBuilderBean, List<RetrofitInterceptorExtension> interceptorExtensions) {
        this.clazz = clazz;
        this.env = env;
        this.globalRetrofitBuilderBean = globalRetrofitBuilderBean;
        this.interceptorExtensions = interceptorExtensions;
    }

    @Override
    public RetrofitApiServiceBean generate() {
        Class<?> retrofitBuilderClazz = getParentRetrofitBuilderClazz();
        RetrofitApiServiceBean retrofitApiServiceBean = new RetrofitApiServiceBean();
        retrofitApiServiceBean.setSelfClazz(clazz);
        retrofitApiServiceBean.setParentClazz(retrofitBuilderClazz);
        //将RetrofitBuilder注解信息注入到RetrofitBuilderBean中
        RetrofitBuilderBean retrofitBuilderBean = getRetrofitBuilder(retrofitBuilderClazz, globalRetrofitBuilderBean);
        retrofitApiServiceBean.setRetrofitBuilder(retrofitBuilderBean);
        Set<RetrofitInterceptor> interceptors = getInterceptors(retrofitBuilderClazz);
        Set<RetrofitInterceptor> myInterceptors = getInterceptors(clazz);
        if (interceptorExtensions != null) {
            for (RetrofitInterceptorExtension interceptorExtension : interceptorExtensions) {
                try {
                    RetrofitInterceptor annotation = interceptorExtension.createAnnotation().getAnnotation(RetrofitInterceptor.class);
                    myInterceptors.add(annotation);
                } catch (NullPointerException ignored) {
                }
            }
        }
        retrofitApiServiceBean.setMyInterceptors(myInterceptors);
        retrofitApiServiceBean.setInterceptors(interceptors);
        RetrofitUrl retrofitUrl = getRetrofitUrl(retrofitBuilderBean);
        retrofitApiServiceBean.setRetrofitUrl(retrofitUrl);
        return retrofitApiServiceBean;
    }

    private RetrofitBuilderBean getRetrofitBuilder(Class<?> retrofitBuilderClazz, RetrofitBuilderBean globalRetrofitBuilderBean) {
        RetrofitBuilder retrofitBuilderAnnotation = retrofitBuilderClazz.getDeclaredAnnotation(RetrofitBuilder.class);
        RetrofitBuilderBean retrofitBuilderBean = new RetrofitBuilderBean();
        if (retrofitBuilderAnnotation.denyGlobalConfig()) {
            setRetrofitBuilderBeanByAnnotation(retrofitBuilderAnnotation, retrofitBuilderBean);
        } else {
            if (globalRetrofitBuilderBean.isEnable()) {
                retrofitBuilderBean.setEnable(true);
                if (globalRetrofitBuilderBean.getOverwriteType() == OverrideRule.GLOBAL_FIRST) {
                    retrofitBuilderBean.setBaseUrl(StringUtils.isNotBlank(globalRetrofitBuilderBean.getBaseUrl()) ? globalRetrofitBuilderBean.getBaseUrl() : retrofitBuilderAnnotation.baseUrl());
                    retrofitBuilderBean.setClient(globalRetrofitBuilderBean.getClient() != null ? globalRetrofitBuilderBean.getClient() : retrofitBuilderAnnotation.client());
                    retrofitBuilderBean.setCallbackExecutor(globalRetrofitBuilderBean.getCallbackExecutor() != null ? globalRetrofitBuilderBean.getCallbackExecutor() : retrofitBuilderAnnotation.callbackExecutor());
                    retrofitBuilderBean.setAddCallAdapterFactory(globalRetrofitBuilderBean.getAddCallAdapterFactory() != null ? globalRetrofitBuilderBean.getAddCallAdapterFactory() : retrofitBuilderAnnotation.addCallAdapterFactory());
                    retrofitBuilderBean.setAddConverterFactory(globalRetrofitBuilderBean.getAddConverterFactory() != null ? globalRetrofitBuilderBean.getAddConverterFactory() : retrofitBuilderAnnotation.addConverterFactory());
                    retrofitBuilderBean.setValidateEagerly(globalRetrofitBuilderBean.getValidateEagerly() != null ? globalRetrofitBuilderBean.getValidateEagerly() : retrofitBuilderAnnotation.validateEagerly());
                    retrofitBuilderBean.setCallFactory(globalRetrofitBuilderBean.getCallFactory() != null ? globalRetrofitBuilderBean.getCallFactory() : retrofitBuilderAnnotation.callFactory());
                } else {
                    retrofitBuilderBean.setBaseUrl(StringUtils.isNotBlank(retrofitBuilderAnnotation.baseUrl()) ? retrofitBuilderAnnotation.baseUrl() : globalRetrofitBuilderBean.getBaseUrl());
                    retrofitBuilderBean.setClient(!retrofitBuilderAnnotation.client().getName().equals(BaseOkHttpClientBuilder.class.getName()) ? retrofitBuilderAnnotation.client() : globalRetrofitBuilderBean.getClient() == null ? retrofitBuilderAnnotation.client() : globalRetrofitBuilderBean.getClient());
                    retrofitBuilderBean.setCallbackExecutor(!retrofitBuilderAnnotation.callbackExecutor().getName().equals(BaseCallBackExecutorBuilder.class.getName()) ? retrofitBuilderAnnotation.callbackExecutor() : globalRetrofitBuilderBean.getCallbackExecutor() == null ? retrofitBuilderAnnotation.callbackExecutor() : globalRetrofitBuilderBean.getCallbackExecutor());
                    retrofitBuilderBean.setAddCallAdapterFactory(retrofitBuilderAnnotation.addCallAdapterFactory().length != 0 ? retrofitBuilderAnnotation.addCallAdapterFactory() : globalRetrofitBuilderBean.getAddCallAdapterFactory());
                    retrofitBuilderBean.setAddConverterFactory(retrofitBuilderAnnotation.addConverterFactory().length != 0 ? retrofitBuilderAnnotation.addConverterFactory() : globalRetrofitBuilderBean.getAddConverterFactory());
                    retrofitBuilderBean.setValidateEagerly(retrofitBuilderAnnotation.validateEagerly() != null ? retrofitBuilderAnnotation.validateEagerly() : globalRetrofitBuilderBean.getValidateEagerly());
                    retrofitBuilderBean.setCallFactory(!retrofitBuilderAnnotation.callFactory().getName().equals(BaseCallFactoryBuilder.class.getName()) ? retrofitBuilderAnnotation.callFactory() : globalRetrofitBuilderBean.getCallFactory() == null ? retrofitBuilderAnnotation.callFactory() : globalRetrofitBuilderBean.getCallFactory());
                }
            } else {
                setRetrofitBuilderBeanByAnnotation(retrofitBuilderAnnotation, retrofitBuilderBean);
            }
        }
        return retrofitBuilderBean;
    }

    private void setRetrofitBuilderBeanByAnnotation(RetrofitBuilder retrofitBuilderAnnotation, RetrofitBuilderBean retrofitBuilderBean) {
        retrofitBuilderBean.setEnable(false);
        retrofitBuilderBean.setBaseUrl(retrofitBuilderAnnotation.baseUrl());
        retrofitBuilderBean.setClient(retrofitBuilderAnnotation.client());
        retrofitBuilderBean.setCallbackExecutor(retrofitBuilderAnnotation.callbackExecutor());
        retrofitBuilderBean.setAddCallAdapterFactory(retrofitBuilderAnnotation.addCallAdapterFactory());
        retrofitBuilderBean.setAddConverterFactory(retrofitBuilderAnnotation.addConverterFactory());
        retrofitBuilderBean.setValidateEagerly(retrofitBuilderAnnotation.validateEagerly());
        retrofitBuilderBean.setCallFactory(retrofitBuilderAnnotation.callFactory());
    }

    private RetrofitUrl getRetrofitUrl(RetrofitBuilderBean retrofitBuilderBean) {
        final RetrofitUrlPrefix retrofitUrlPrefix = clazz.getDeclaredAnnotation(RetrofitUrlPrefix.class);
        final RetrofitDynamicBaseUrl retrofitDynamicBaseUrl = clazz.getDeclaredAnnotation(RetrofitDynamicBaseUrl.class);
        String retrofitDynamicBaseUrlValue = retrofitDynamicBaseUrl == null ? null : retrofitDynamicBaseUrl.value();
        return new RetrofitUrl(retrofitBuilderBean.getBaseUrl(),
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
