package com.jd.rec.nl.service.common.quartet.filter;

import com.jd.rec.nl.core.exception.InvalidDataException;
import com.jd.rec.nl.core.guice.ApplicationContext;
import com.jd.rec.nl.core.guice.InjectorService;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author linmx
 * @date 2018/10/11
 */
public abstract class MethodFilterInterceptor implements MethodInterceptor {

    private static final Logger LOGGER = getLogger(MethodFilterInterceptor.class);

    protected List<MethodFilter> preFilters = new ArrayList<>();

    protected List<MethodFilter> postFilters = new ArrayList<>();

    protected List<MethodFilter> roundFilters = new ArrayList<>();

    private boolean init = false;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        initFilters();
        Object[] args = invocation.getArguments();
        for (MethodFilter filter : preFilters) {
            if (!filter.preFilter(args)) {
                throw new InvalidDataException(filter.invalidMessage());
            }
        }
        for (MethodFilter filter : roundFilters) {
            if (!filter.preFilter(args)) {
                throw new InvalidDataException(filter.invalidMessage());
            }
        }
        Object ret = invocation.proceed();
        for (MethodFilter filter : postFilters) {
            if (!filter.postFilter(ret, args)) {
                throw new InvalidDataException(filter.invalidMessage());
            }
        }
        for (MethodFilter filter : roundFilters) {
            if (!filter.postFilter(ret, args)) {
                throw new InvalidDataException(filter.invalidMessage());
            }
        }
        return ret;
    }

    /**
     * 需要拦截的方法
     *
     * @return
     */
    public abstract Method interceptMethod();

    protected void initFilters() {
        if (!init) {
            synchronized (this) {
                if (!init) {
                    Method targetMethod = interceptMethod();
                    Class targetClass = targetMethod.getDeclaringClass();
                    Set<MethodFilter> definedFilters = ApplicationContext.getRawDefinedBeans(MethodFilter.class);
                    definedFilters.stream().filter(filter -> {
                        Method method = filter.filterMethod();
                        return method.getDeclaringClass() == targetClass && (Arrays.stream(targetMethod.getParameterTypes())
                                .allMatch(paramClass -> Arrays.asList(method.getParameterTypes()).contains(paramClass)));
                    }).forEach(filter -> {
                        InjectorService.getCommonInjector().injectMembers(filter);
                        if (filter.filterType() == MethodFilter.FilterType.pre) {
                            preFilters.add(filter);
                        } else if (filter.filterType() == MethodFilter.FilterType.post) {
                            postFilters.add(filter);
                        } else if (filter.filterType() == MethodFilter.FilterType.round) {
                            roundFilters.add(filter);
                        } else {
                            LOGGER.error("{} has a unknown type!", filter.filterType());
                        }
                    });
                    init = true;
                }
            }
        }
    }
}
