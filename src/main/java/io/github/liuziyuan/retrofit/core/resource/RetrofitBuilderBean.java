package io.github.liuziyuan.retrofit.core.resource;

import io.github.liuziyuan.retrofit.core.OverrideRule;
import io.github.liuziyuan.retrofit.core.RetrofitBuilderExtension;
import io.github.liuziyuan.retrofit.core.annotation.RetrofitBuilder;
import io.github.liuziyuan.retrofit.core.builder.*;
import io.github.liuziyuan.retrofit.core.util.BooleanUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

@Getter
@Setter
public class RetrofitBuilderBean {
    private boolean enable = false;

    private OverrideRule overwriteType = OverrideRule.GLOBAL_FIRST;

    private String baseUrl = "";

    private Class<? extends BaseCallAdapterFactoryBuilder>[] addCallAdapterFactory;

    private Class<? extends BaseConverterFactoryBuilder>[] addConverterFactory;

    private Class<? extends BaseOkHttpClientBuilder> client;

    private Class<? extends BaseCallBackExecutorBuilder> callbackExecutor;

    private Class<? extends BaseCallFactoryBuilder> callFactory;

    private boolean validateEagerly;

    public RetrofitBuilderBean(Class<?> retrofitBuilderClazz, RetrofitBuilderExtension globalRetrofitBuilderExtension) {
        RetrofitBuilder retrofitBuilderAnnotation = retrofitBuilderClazz.getDeclaredAnnotation(RetrofitBuilder.class);
        if (retrofitBuilderAnnotation.globalOverwriteRule().equals(OverrideRule.LOCAL_ONLY)) {
            setRetrofitBuilderBeanByLocalOnly(retrofitBuilderAnnotation);
        } else if (retrofitBuilderAnnotation.globalOverwriteRule().equals(OverrideRule.LOCAL_FIRST) && globalRetrofitBuilderExtension.enable()) {
            setRetrofitBuilderBeanByLocalFirst(retrofitBuilderAnnotation, globalRetrofitBuilderExtension);
        } else if (retrofitBuilderAnnotation.globalOverwriteRule().equals(OverrideRule.MERGE) && globalRetrofitBuilderExtension.enable()) {
            setRetrofitBuilderBeanByMerge(retrofitBuilderAnnotation, globalRetrofitBuilderExtension);
        } else if (retrofitBuilderAnnotation.globalOverwriteRule().equals(OverrideRule.GLOBAL_FIRST) && globalRetrofitBuilderExtension.enable()) {
            setRetrofitBuilderBeanByGlobalFirst(retrofitBuilderAnnotation, globalRetrofitBuilderExtension);
        } else if (retrofitBuilderAnnotation.globalOverwriteRule().equals(OverrideRule.GLOBAL_ONLY) && globalRetrofitBuilderExtension.enable()) {
            setRetrofitBuilderBeanByGlobalOnly(globalRetrofitBuilderExtension);
        } else {
            setRetrofitBuilderBeanByLocalOnly(retrofitBuilderAnnotation);
        }
    }

    private void setRetrofitBuilderBeanByLocalFirst(RetrofitBuilder retrofitBuilderAnnotation, RetrofitBuilderExtension globalRetrofitBuilderExtension) {
        this.setEnable(true);
        this.setBaseUrl(StringUtils.isNotBlank(retrofitBuilderAnnotation.baseUrl()) ? retrofitBuilderAnnotation.baseUrl() : globalRetrofitBuilderExtension.globalBaseUrl());
        this.setClient(!retrofitBuilderAnnotation.client().getName().equals(BaseOkHttpClientBuilder.class.getName()) ? retrofitBuilderAnnotation.client() : globalRetrofitBuilderExtension.globalOkHttpClientBuilderClazz() == null ? retrofitBuilderAnnotation.client() : globalRetrofitBuilderExtension.globalOkHttpClientBuilderClazz());
        this.setCallbackExecutor(!retrofitBuilderAnnotation.callbackExecutor().getName().equals(BaseCallBackExecutorBuilder.class.getName()) ? retrofitBuilderAnnotation.callbackExecutor() : globalRetrofitBuilderExtension.globalCallBackExecutorBuilderClazz() == null ? retrofitBuilderAnnotation.callbackExecutor() : globalRetrofitBuilderExtension.globalCallBackExecutorBuilderClazz());
        this.setAddCallAdapterFactory(retrofitBuilderAnnotation.addCallAdapterFactory().length != 0 ? retrofitBuilderAnnotation.addCallAdapterFactory() : globalRetrofitBuilderExtension.globalCallAdapterFactoryBuilderClazz());
        this.setAddConverterFactory(retrofitBuilderAnnotation.addConverterFactory().length != 0 ? retrofitBuilderAnnotation.addConverterFactory() : globalRetrofitBuilderExtension.globalConverterFactoryBuilderClazz());
        this.setValidateEagerly(retrofitBuilderAnnotation.validateEagerly());
        this.setCallFactory(!retrofitBuilderAnnotation.callFactory().getName().equals(BaseCallFactoryBuilder.class.getName()) ? retrofitBuilderAnnotation.callFactory() : globalRetrofitBuilderExtension.globalCallFactoryBuilderClazz() == null ? retrofitBuilderAnnotation.callFactory() : globalRetrofitBuilderExtension.globalCallFactoryBuilderClazz());
    }

    /**
     * if merge, default date is local_first, then merge global CallAdapterFactory and ConverterFactory to local_first
     * @param retrofitBuilderAnnotation
     * @param globalRetrofitBuilderExtension
     */
    private void setRetrofitBuilderBeanByMerge(RetrofitBuilder retrofitBuilderAnnotation, RetrofitBuilderExtension globalRetrofitBuilderExtension) {
        this.setEnable(true);
        this.setBaseUrl(StringUtils.isNotBlank(retrofitBuilderAnnotation.baseUrl()) ? retrofitBuilderAnnotation.baseUrl() : globalRetrofitBuilderExtension.globalBaseUrl());
        this.setClient(!retrofitBuilderAnnotation.client().getName().equals(BaseOkHttpClientBuilder.class.getName()) ? retrofitBuilderAnnotation.client() : globalRetrofitBuilderExtension.globalOkHttpClientBuilderClazz() == null ? retrofitBuilderAnnotation.client() : globalRetrofitBuilderExtension.globalOkHttpClientBuilderClazz());
        this.setCallbackExecutor(!retrofitBuilderAnnotation.callbackExecutor().getName().equals(BaseCallBackExecutorBuilder.class.getName()) ? retrofitBuilderAnnotation.callbackExecutor() : globalRetrofitBuilderExtension.globalCallBackExecutorBuilderClazz() == null ? retrofitBuilderAnnotation.callbackExecutor() : globalRetrofitBuilderExtension.globalCallBackExecutorBuilderClazz());
        this.setValidateEagerly(retrofitBuilderAnnotation.validateEagerly());
        this.setCallFactory(!retrofitBuilderAnnotation.callFactory().getName().equals(BaseCallFactoryBuilder.class.getName()) ? retrofitBuilderAnnotation.callFactory() : globalRetrofitBuilderExtension.globalCallFactoryBuilderClazz() == null ? retrofitBuilderAnnotation.callFactory() : globalRetrofitBuilderExtension.globalCallFactoryBuilderClazz());
        this.setAddCallAdapterFactory(getCallAdapterFactories(retrofitBuilderAnnotation, globalRetrofitBuilderExtension));
        this.setAddConverterFactory(getConverterFactories(retrofitBuilderAnnotation, globalRetrofitBuilderExtension));
    }

    private Class<? extends BaseCallAdapterFactoryBuilder>[] getCallAdapterFactories(RetrofitBuilder retrofitBuilderAnnotation, RetrofitBuilderExtension globalRetrofitBuilderExtension) {
        Set<Class<? extends BaseCallAdapterFactoryBuilder>> addCallAdapterFactoryBuilderList = new HashSet<>();
        if (globalRetrofitBuilderExtension.globalConverterFactoryBuilderClazz() != null) {
            addCallAdapterFactoryBuilderList.addAll(Arrays.asList(globalRetrofitBuilderExtension.globalCallAdapterFactoryBuilderClazz()));
        }
        if (retrofitBuilderAnnotation.addConverterFactory().length != 0) {
            addCallAdapterFactoryBuilderList.addAll(Arrays.asList(retrofitBuilderAnnotation.addCallAdapterFactory()));
        }
        return addCallAdapterFactoryBuilderList.toArray(new Class[0]);
    }

    private Class<? extends BaseConverterFactoryBuilder>[] getConverterFactories(RetrofitBuilder retrofitBuilderAnnotation, RetrofitBuilderExtension globalRetrofitBuilderExtension) {
        Set<Class<? extends BaseConverterFactoryBuilder>> addConverterFactoryBuilderList = new HashSet<>();
        if (globalRetrofitBuilderExtension.globalConverterFactoryBuilderClazz() != null) {
            addConverterFactoryBuilderList.addAll(Arrays.asList(globalRetrofitBuilderExtension.globalConverterFactoryBuilderClazz()));
        }
        if (retrofitBuilderAnnotation.addConverterFactory().length != 0) {
            addConverterFactoryBuilderList.addAll(Arrays.asList(retrofitBuilderAnnotation.addConverterFactory()));
        }
        return addConverterFactoryBuilderList.toArray(new Class[0]);
    }

    private void setRetrofitBuilderBeanByGlobalFirst(RetrofitBuilder retrofitBuilderAnnotation, RetrofitBuilderExtension globalRetrofitBuilderExtension) {
        this.setEnable(true);
        this.setBaseUrl(StringUtils.isNotBlank(globalRetrofitBuilderExtension.globalBaseUrl()) ? globalRetrofitBuilderExtension.globalBaseUrl() : retrofitBuilderAnnotation.baseUrl());
        this.setClient(globalRetrofitBuilderExtension.globalOkHttpClientBuilderClazz() != null ? globalRetrofitBuilderExtension.globalOkHttpClientBuilderClazz() : retrofitBuilderAnnotation.client());
        this.setCallbackExecutor(globalRetrofitBuilderExtension.globalCallBackExecutorBuilderClazz() != null ? globalRetrofitBuilderExtension.globalCallBackExecutorBuilderClazz() : retrofitBuilderAnnotation.callbackExecutor());
        this.setAddCallAdapterFactory(globalRetrofitBuilderExtension.globalCallAdapterFactoryBuilderClazz() != null ? globalRetrofitBuilderExtension.globalCallAdapterFactoryBuilderClazz() : retrofitBuilderAnnotation.addCallAdapterFactory());
        this.setAddConverterFactory(globalRetrofitBuilderExtension.globalConverterFactoryBuilderClazz() != null ? globalRetrofitBuilderExtension.globalConverterFactoryBuilderClazz() : retrofitBuilderAnnotation.addConverterFactory());
        this.setValidateEagerly(globalRetrofitBuilderExtension.globalValidateEagerly() != null ? BooleanUtil.transformToBoolean(globalRetrofitBuilderExtension.globalValidateEagerly()) : retrofitBuilderAnnotation.validateEagerly());
        this.setCallFactory(globalRetrofitBuilderExtension.globalCallFactoryBuilderClazz() != null ? globalRetrofitBuilderExtension.globalCallFactoryBuilderClazz() : retrofitBuilderAnnotation.callFactory());
    }

    private void setRetrofitBuilderBeanByLocalOnly(RetrofitBuilder retrofitBuilderAnnotation) {
        this.setEnable(false);
        this.setBaseUrl(retrofitBuilderAnnotation.baseUrl());
        this.setClient(retrofitBuilderAnnotation.client());
        this.setCallbackExecutor(retrofitBuilderAnnotation.callbackExecutor());
        this.setAddCallAdapterFactory(retrofitBuilderAnnotation.addCallAdapterFactory());
        this.setAddConverterFactory(retrofitBuilderAnnotation.addConverterFactory());
        this.setValidateEagerly(retrofitBuilderAnnotation.validateEagerly());
        this.setCallFactory(retrofitBuilderAnnotation.callFactory());
    }

    private void setRetrofitBuilderBeanByGlobalOnly(RetrofitBuilderExtension globalRetrofitBuilderExtension) {
        this.setEnable(globalRetrofitBuilderExtension.enable());
        this.setBaseUrl(globalRetrofitBuilderExtension.globalBaseUrl());
        this.setClient(globalRetrofitBuilderExtension.globalOkHttpClientBuilderClazz());
        this.setCallbackExecutor(globalRetrofitBuilderExtension.globalCallBackExecutorBuilderClazz());
        this.setAddCallAdapterFactory(globalRetrofitBuilderExtension.globalCallAdapterFactoryBuilderClazz());
        this.setAddConverterFactory(globalRetrofitBuilderExtension.globalConverterFactoryBuilderClazz());
        this.setValidateEagerly(BooleanUtil.transformToBoolean(globalRetrofitBuilderExtension.globalValidateEagerly()));
        this.setCallFactory(globalRetrofitBuilderExtension.globalCallFactoryBuilderClazz());
    }
}
