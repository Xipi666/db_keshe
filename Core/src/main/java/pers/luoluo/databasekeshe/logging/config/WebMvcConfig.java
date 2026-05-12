package pers.luoluo.databasekeshe.logging.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RuntimeLogInterceptor runtimeLogInterceptor;

    public WebMvcConfig(RuntimeLogInterceptor runtimeLogInterceptor) {
        this.runtimeLogInterceptor = runtimeLogInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(runtimeLogInterceptor).addPathPatterns("/api/**");
    }
}
