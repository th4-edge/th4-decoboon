package com.th4.edge.decoboon.common.configure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JSON 数值精度配置。
 *
 * <p>雪花 ID 超出 JavaScript 安全整数范围，统一按字符串输出，避免小程序端
 * JSON.parse 后发生精度截断。反序列化仍兼容字符串和数值。</p>
 */
@Configuration
public class JacksonLongConfigure {

    @Bean
    static BeanPostProcessor longToStringObjectMapperPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof ObjectMapper objectMapper) {
                    SimpleModule module = new SimpleModule("LongToString");
                    module.addSerializer(Long.class, ToStringSerializer.instance);
                    module.addSerializer(Long.TYPE, ToStringSerializer.instance);
                    objectMapper.registerModule(module);
                }
                return bean;
            }
        };
    }
}
