package pers.luoluo.databasekeshe.logging.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import pers.luoluo.databasekeshe.logging.service.RuntimeLogService;

@Component
public class RuntimeLogInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTRIBUTE = "runtimeLogStartTime";

    private final RuntimeLogService runtimeLogService;

    public RuntimeLogInterceptor(RuntimeLogService runtimeLogService) {
        this.runtimeLogService = runtimeLogService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception
    ) {
        if (!request.getRequestURI().startsWith("/api/")) {
            return;
        }

        long elapsed = elapsedMillis(request);
        String context = request.getMethod() + " " + request.getRequestURI()
                + " status=" + response.getStatus()
                + " elapsedMs=" + elapsed;

        if (exception != null || response.getStatus() >= 500) {
            String message = exception == null ? "后端请求处理失败" : exception.getMessage();
            runtimeLogService.error(message, context);
        } else if (response.getStatus() >= 400) {
            runtimeLogService.warn("后端请求被拒绝", context);
        } else {
            runtimeLogService.info("后端请求完成", context);
        }
    }

    private long elapsedMillis(HttpServletRequest request) {
        Object startTime = request.getAttribute(START_TIME_ATTRIBUTE);
        if (startTime instanceof Long value) {
            return System.currentTimeMillis() - value;
        }
        return 0;
    }
}
