package com.lxq.spring_api_chat.common.config;

import com.lxq.spring_api_chat.common.interceptor.MdcInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置类
 * 注册拦截器等Web相关配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 添加拦截器
     * 注册MDC拦截器实现链路追踪
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new MdcInterceptor())
                .addPathPatterns("/**")  // 拦截所有请求
                .order(1);  // 设置拦截器优先级，数字越小优先级越高
    }
}
