package io.github.liuziyuan.retrofit.core;

import io.github.liuziyuan.retrofit.core.annotation.RetrofitBase;
import io.github.liuziyuan.retrofit.core.annotation.RetrofitBuilder;
import io.github.liuziyuan.retrofit.core.annotation.RetrofitComponent;
import io.github.liuziyuan.retrofit.core.exception.ProxyTypeIsNotInterfaceException;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Scan retrofit API resources using @RetrofitBuilder
 *
 * @author liuziyuan
 */
@Slf4j
public abstract class RetrofitResourceScanner {

    private String[] basePackages;

    public Set<Class<?>> doScan(String... basePackages) {
        this.basePackages = basePackages;
        Reflections reflections = getReflections(this.basePackages);
        final Set<Class<?>> retrofitBuilderClasses = getRetrofitResourceClasses(reflections, RetrofitBuilder.class);
        final Set<Class<?>> retrofitBaseApiClasses = getRetrofitResourceClasses(reflections, RetrofitBase.class);
        retrofitBuilderClasses.addAll(retrofitBaseApiClasses);
        return retrofitBuilderClasses;
    }

    public Set<Class<?>> scanRetrofitComponentClasses(@Nullable String interfaceSampleName) {
        Set<Class<?>> retrofitResources = this.getRetrofitResource(RetrofitComponent.class);
        Set<Class<?>> results = new HashSet<>();
        for (Class<?> retrofitResource : retrofitResources) {
            if (Arrays.stream(retrofitResource.getInterfaces()).anyMatch(c -> interfaceSampleName.equalsIgnoreCase(c.getName()))) {
                results.add(retrofitResource);
            }
        }
        return results;
    }

    public <T extends GlobalParamConfig> T getRetrofitComponentGlobalParamConfigInstance() {
        Set<Class<?>> retrofitResources = this.getRetrofitResource(RetrofitComponent.class);
        for (Class<?> retrofitResource : retrofitResources) {
            if (Arrays.stream(retrofitResource.getInterfaces()).anyMatch(c -> GlobalParamConfig.class.getName().equalsIgnoreCase(c.getName()))) {
                try {
                    return (T) retrofitResource.newInstance();
                } catch (IllegalAccessException | InstantiationException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }

    public Set<Class<?>> getRetrofitResource(Class<? extends Annotation> clazz) {
        Reflections reflections = getReflections(this.basePackages);
        return reflections.getTypesAnnotatedWith(clazz);
    }

    public Set<Class<?>> getRetrofitResource(Class<? extends Annotation> clazz, String... basePackages) {
        Reflections reflections = getReflections(basePackages);
        return reflections.getTypesAnnotatedWith(clazz);
    }

    public Set<Class<?>> getRetrofitResourceClasses(Reflections reflections, Class<? extends Annotation> annotationClass) {
        final Set<Class<?>> classSet = reflections.getTypesAnnotatedWith(annotationClass);
        Iterator<Class<?>> iterator = classSet.iterator();
        while (iterator.hasNext()) {
            Class<?> clazz = iterator.next();
            if (!clazz.isInterface()) {
                // 使用iterator的remove方法安全地移除元素
                iterator.remove();
                log.warn("[{}] requires an interface type", clazz.getName());
            }
        }
        return classSet;
    }

    private Reflections getReflections(String[] basePackages) {
        ConfigurationBuilder configuration;
        if (basePackages.length == 0) {
            configuration = new ConfigurationBuilder().forPackages("");
        } else {
            Pattern filterPattern = Pattern.compile(Arrays.stream(basePackages)
                    .map(s -> s.replace(".", "/"))
                    .collect(Collectors.joining("|", ".*?(", ").*?")));
            log.debug("Scanner Pattern : {}", filterPattern.pattern());
            configuration = new ConfigurationBuilder().forPackages(basePackages).filterInputsBy(s ->
            {
                log.debug("Filter inputs {}", s);
                return filterPattern.matcher(s).matches();
            });

        }
        return new Reflections(configuration);
    }

}
