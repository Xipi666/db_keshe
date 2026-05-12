package pers.luoluo.databasekeshe.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pers.luoluo.databasekeshe.auth.exception.AuthException;
import pers.luoluo.databasekeshe.common.dto.ApiErrorResponse;
import pers.luoluo.databasekeshe.logging.service.RuntimeLogService;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final RuntimeLogService runtimeLogService;

    public GlobalExceptionHandler(RuntimeLogService runtimeLogService) {
        this.runtimeLogService = runtimeLogService;
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthException(AuthException exception) {
        runtimeLogService.warn(exception.getMessage(), "业务异常 status=" + exception.status().value());
        return ResponseEntity.status(exception.status()).body(new ApiErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception exception) {
        runtimeLogService.error(exception.getMessage(), exception.getClass().getName());
        return ResponseEntity.internalServerError().body(new ApiErrorResponse("服务端处理失败"));
    }
}
