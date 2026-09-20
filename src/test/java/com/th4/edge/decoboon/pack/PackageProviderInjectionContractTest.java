package com.th4.edge.decoboon.pack;

import com.th4.edge.framework.data.jpa.inject.annotation.ModelSaveInject;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 防止套餐和报价调整 Provider 再次漏掉请求实体注入。 */
class PackageProviderInjectionContractTest {

    @Test
    void shouldInjectRequestEntityForAllPackageAndQuoteLineWrites() {
        List<Class<?>> providers = List.of(
                com.th4.edge.decoboon.pack.service.templates.save.support.Save.class,
                com.th4.edge.decoboon.pack.service.templates.update.support.Update.class,
                com.th4.edge.decoboon.pack.service.sections.save.support.Save.class,
                com.th4.edge.decoboon.pack.service.lines.save.support.Save.class,
                com.th4.edge.decoboon.pack.service.templateitems.save.support.Save.class,
                com.th4.edge.decoboon.quote.service.lines.save.support.Save.class);

        for (Class<?> provider : providers) {
            Method call = Arrays.stream(provider.getMethods())
                    .filter(method -> method.getName().equals("call"))
                    .findFirst()
                    .orElseThrow();
            boolean injected = Arrays.stream(call.getParameterAnnotations())
                    .flatMap(Arrays::stream)
                    .anyMatch(annotation -> annotation.annotationType() == ModelSaveInject.class);
            assertThat(injected)
                    .as(provider.getSimpleName() + ".call must declare @ModelSaveInject")
                    .isTrue();
        }
    }
}
