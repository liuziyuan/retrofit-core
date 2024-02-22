package io.github.liuziyuan.retrofit.core.generator;

import io.github.liuziyuan.retrofit.core.RetrofitResourceContext;
import io.github.liuziyuan.retrofit.core.annotation.InterceptorType;
import io.github.liuziyuan.retrofit.core.annotation.RetrofitBuilder;
import io.github.liuziyuan.retrofit.core.annotation.RetrofitInterceptor;
import io.github.liuziyuan.retrofit.core.builder.*;
import io.github.liuziyuan.retrofit.core.extension.BaseInterceptor;
import io.github.liuziyuan.retrofit.core.extension.DynamicBaseUrlInterceptor;
import io.github.liuziyuan.retrofit.core.extension.UrlOverWriteInterceptor;
import io.github.liuziyuan.retrofit.core.resource.RetrofitClientBean;
import io.github.liuziyuan.retrofit.core.util.CollectionUtils;
import lombok.SneakyThrows;
import okhttp3.Call;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;

import java.util.*;
import java.util.concurrent.Executor;

/**
 * Generate RetrofitBuilder instance
 *
 * @author liuziyuan
 */
public abstract class RetrofitBuilderGenerator implements Generator<Retrofit.Builder> {
    private final RetrofitClientBean clientBean;
    private final RetrofitResourceContext context;
    private final Retrofit.Builder builder;


    public RetrofitBuilderGenerator(RetrofitClientBean clientBean, RetrofitResourceContext context) {
        this.builder = new Retrofit.Builder();
        this.clientBean = clientBean;
        this.context = context;
    }

    @Override
    public Retrofit.Builder generate() {
        setBaseUrl();
        setRetrofitCallAdapterFactory();
        setRetrofitConverterFactory();
        setCallBackExecutor();
        setValidateEagerly();
        setRetrofitOkHttpClient();
        setCallFactory();
        return builder;
    }


    private void setBaseUrl() {
        builder.baseUrl(clientBean.getRealHostUrl());
    }

    public abstract BaseCallFactoryBuilder buildInjectionCallFactory(Class<? extends BaseCallFactoryBuilder> clazz);

    public abstract BaseCallBackExecutorBuilder buildInjectionCallBackExecutor(Class<? extends BaseCallBackExecutorBuilder> clazz);

    public abstract BaseOkHttpClientBuilder buildInjectionOkHttpClient(Class<? extends BaseOkHttpClientBuilder> clazz);

    public abstract BaseInterceptor buildInjectionInterceptor(Class<? extends BaseInterceptor> clazz);

    public abstract BaseCallAdapterFactoryBuilder buildInjectionCallAdapterFactor(Class<? extends BaseCallAdapterFactoryBuilder> clazz);

    public abstract BaseConverterFactoryBuilder buildInjectionConverterFactory(Class<? extends BaseConverterFactoryBuilder> clazz);
    private void setValidateEagerly() {
        final RetrofitBuilder retrofitBuilder = clientBean.getRetrofitBuilder();
        final boolean validateEagerly = retrofitBuilder.validateEagerly();
        builder.validateEagerly(validateEagerly);
    }

    private void setCallFactory() {
        final RetrofitBuilder retrofitBuilder = clientBean.getRetrofitBuilder();
        final Class<? extends BaseCallFactoryBuilder> callFactoryBuilderClazz = retrofitBuilder.callFactory();
        CallFactoryGenerator callFactoryGenerator = new CallFactoryGenerator(callFactoryBuilderClazz) {
            @Override
            public BaseCallFactoryBuilder buildInjectionObject(Class<? extends BaseCallFactoryBuilder> clazz) {
                return buildInjectionCallFactory(clazz);
            }
        };
        final Call.Factory factory = callFactoryGenerator.generate();
        if (factory != null) {
            builder.callFactory(factory);
        }
    }

    private void setCallBackExecutor() {
        final RetrofitBuilder retrofitBuilder = clientBean.getRetrofitBuilder();
        final Class<? extends BaseCallBackExecutorBuilder> callbackExecutorBuilderClazz = retrofitBuilder.callbackExecutor();
        CallBackExecutorGenerator callBackExecutorGenerator = new CallBackExecutorGenerator(callbackExecutorBuilderClazz) {
            @Override
            public BaseCallBackExecutorBuilder buildInjectionObject(Class<? extends BaseCallBackExecutorBuilder> clazz) {
                return buildInjectionCallBackExecutor(clazz);
            }
        };
        final Executor executor = callBackExecutorGenerator.generate();
        if (executor != null) {
            builder.callbackExecutor(executor);
        }
    }

    private void setRetrofitCallAdapterFactory() {
        final RetrofitBuilder retrofitBuilder = clientBean.getRetrofitBuilder();
        final List<CallAdapter.Factory> callAdapterFactories = getCallAdapterFactories(retrofitBuilder.addCallAdapterFactory());
        if (!CollectionUtils.isEmpty(callAdapterFactories)) {
            callAdapterFactories.forEach(builder::addCallAdapterFactory);
        }
    }

    private void setRetrofitConverterFactory() {
        final RetrofitBuilder retrofitBuilder = clientBean.getRetrofitBuilder();
        final List<Converter.Factory> converterFactories = getConverterFactories(retrofitBuilder.addConverterFactory());
        if (!CollectionUtils.isEmpty(converterFactories)) {
            converterFactories.forEach(builder::addConverterFactory);
        }
    }

    @SneakyThrows
    private void setRetrofitOkHttpClient() {
        final RetrofitBuilder retrofitBuilder = clientBean.getRetrofitBuilder();
        Set<RetrofitInterceptor> allInterceptors = new LinkedHashSet<>();
        allInterceptors.addAll(clientBean.getInterceptors());
        allInterceptors.addAll(clientBean.getInheritedInterceptors());
        final List<RetrofitInterceptor> interceptors = new ArrayList<>(allInterceptors);
        OkHttpClient.Builder okHttpClientBuilder;
        if (retrofitBuilder.client() != null) {
            final OkHttpClientBuilderGenerator clientBuilderGenerator = new OkHttpClientBuilderGenerator(retrofitBuilder.client()) {
                @Override
                public BaseOkHttpClientBuilder buildInjectionObject(Class<? extends BaseOkHttpClientBuilder> clazz) {
                    return buildInjectionOkHttpClient(clazz);
                }
            };
            okHttpClientBuilder = clientBuilderGenerator.generate();
        } else {
            okHttpClientBuilder = new OkHttpClient.Builder();
        }
        okHttpClientBuilder.addInterceptor(new DynamicBaseUrlInterceptor(context));
        okHttpClientBuilder.addInterceptor(new UrlOverWriteInterceptor(context));
        final List<Interceptor> okHttpDefaultInterceptors = getOkHttpInterceptors(interceptors, InterceptorType.DEFAULT);
        final List<Interceptor> okHttpNetworkInterceptors = getOkHttpInterceptors(interceptors, InterceptorType.NETWORK);
        okHttpDefaultInterceptors.forEach(okHttpClientBuilder::addInterceptor);
        okHttpNetworkInterceptors.forEach(okHttpClientBuilder::addNetworkInterceptor);
        builder.client(okHttpClientBuilder.build());
    }

    @SneakyThrows
    private List<Interceptor> getOkHttpInterceptors(List<RetrofitInterceptor> interceptors, InterceptorType type) {
        List<Interceptor> interceptorList = new ArrayList<>();
        OkHttpInterceptorGenerator okHttpInterceptorGenerator;
        interceptors.sort(Comparator.comparing(RetrofitInterceptor::sort));
        for (RetrofitInterceptor interceptor : interceptors) {
            if (interceptor.type() == type) {
                okHttpInterceptorGenerator = new OkHttpInterceptorGenerator(interceptor, context) {
                    @Override
                    public BaseInterceptor buildInjectionObject(Class<? extends BaseInterceptor> clazz) {
                        return buildInjectionInterceptor(clazz);
                    }
                };
                final Interceptor generateInterceptor = okHttpInterceptorGenerator.generate();
                interceptorList.add(generateInterceptor);
            }
        }
        return interceptorList;
    }

    @SneakyThrows
    private List<CallAdapter.Factory> getCallAdapterFactories(Class<? extends BaseCallAdapterFactoryBuilder>[] callAdapterFactoryClasses) {
        List<CallAdapter.Factory> callAdapterFactories = new ArrayList<>();
        CallAdapterFactoryGenerator callAdapterFactoryGenerator;
        for (Class<? extends BaseCallAdapterFactoryBuilder> callAdapterFactoryClazz : callAdapterFactoryClasses) {
            callAdapterFactoryGenerator = new CallAdapterFactoryGenerator(callAdapterFactoryClazz) {
                @Override
                public BaseCallAdapterFactoryBuilder buildInjectionObject(Class<? extends BaseCallAdapterFactoryBuilder> clazz) {
                    return buildInjectionCallAdapterFactor(clazz);
                }
            };
            callAdapterFactories.add(callAdapterFactoryGenerator.generate());
        }
        return callAdapterFactories;
    }

    @SneakyThrows
    private List<Converter.Factory> getConverterFactories(Class<? extends BaseConverterFactoryBuilder>[] converterFactoryBuilderClasses) {
        List<Converter.Factory> converterFactories = new ArrayList<>();
        ConverterFactoryGenerator converterFactoryGenerator;
        for (Class<? extends BaseConverterFactoryBuilder> converterFactoryBuilderClazz : converterFactoryBuilderClasses) {
            converterFactoryGenerator = new ConverterFactoryGenerator(converterFactoryBuilderClazz) {
                @Override
                public BaseConverterFactoryBuilder buildInjectionObject(Class<? extends BaseConverterFactoryBuilder> clazz) {
                    return buildInjectionConverterFactory(clazz);
                }
            };
            converterFactories.add(converterFactoryGenerator.generate());
        }
        return converterFactories;
    }

}
