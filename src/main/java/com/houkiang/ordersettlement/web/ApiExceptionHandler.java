package com.houkiang.ordersettlement.web;

import com.houkiang.ordersettlement.catalog.CouponNotFoundException;
import com.houkiang.ordersettlement.catalog.ProductNotFoundException;
import com.houkiang.ordersettlement.common.exception.BusinessException;
import com.houkiang.ordersettlement.order.exception.InsufficientStockException;
import com.houkiang.ordersettlement.order.exception.IdempotencyConflictException;
import com.houkiang.ordersettlement.order.exception.InvalidIdempotencyKeyException;
import com.houkiang.ordersettlement.order.exception.InvalidOrderStatusTransitionException;
import com.houkiang.ordersettlement.order.exception.OrderNotFoundException;
import com.houkiang.ordersettlement.order.exception.OrderUpdateConflictException;
import com.houkiang.ordersettlement.promotion.exception.CouponNotApplicableException;
import com.houkiang.ordersettlement.promotion.exception.InvalidCouponException;
import com.houkiang.ordersettlement.shipping.exception.UnsupportedDeliveryTypeException;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        List<ApiError.FieldViolation> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldViolation(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(ApiError.FieldViolation::field)
                        .thenComparing(ApiError.FieldViolation::message))
                .toList();
        return ResponseEntity.status(status).headers(headers).contentType(MediaType.APPLICATION_JSON)
                .body(new ApiError("VALIDATION_FAILED", "请求参数校验失败", errors));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String code = switch (status.value()) {
            case 400 -> "INVALID_REQUEST";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 406 -> "NOT_ACCEPTABLE";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            default -> "HTTP_ERROR";
        };
        String message = status.is5xxServerError() ? "服务器处理请求失败" : "请求格式、类型或访问方式不正确";
        return ResponseEntity.status(status).headers(headers).contentType(MediaType.APPLICATION_JSON)
                .body(new ApiError(code, message, List.of()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(BusinessException exception) {
        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
        String code = "BUSINESS_RULE_VIOLATION";
        if (exception instanceof ProductNotFoundException) {
            status = HttpStatus.NOT_FOUND;
            code = "PRODUCT_NOT_FOUND";
        } else if (exception instanceof CouponNotFoundException) {
            status = HttpStatus.NOT_FOUND;
            code = "COUPON_NOT_FOUND";
        } else if (exception instanceof OrderNotFoundException) {
            status = HttpStatus.NOT_FOUND;
            code = "ORDER_NOT_FOUND";
        } else if (exception instanceof InvalidOrderStatusTransitionException) {
            status = HttpStatus.CONFLICT;
            code = "INVALID_ORDER_STATUS_TRANSITION";
        } else if (exception instanceof OrderUpdateConflictException) {
            status = HttpStatus.CONFLICT;
            code = "ORDER_UPDATE_CONFLICT";
        } else if (exception instanceof IdempotencyConflictException) {
            status = HttpStatus.CONFLICT;
            code = "IDEMPOTENCY_CONFLICT";
        } else if (exception instanceof InvalidIdempotencyKeyException) {
            status = HttpStatus.BAD_REQUEST;
            code = "INVALID_IDEMPOTENCY_KEY";
        } else if (exception instanceof InsufficientStockException) {
            status = HttpStatus.CONFLICT;
            code = "INSUFFICIENT_STOCK";
        } else if (exception instanceof CouponNotApplicableException) {
            code = "COUPON_NOT_APPLICABLE";
        } else if (exception instanceof UnsupportedDeliveryTypeException) {
            code = "UNSUPPORTED_DELIVERY";
        } else if (exception instanceof InvalidCouponException) {
            status = HttpStatus.BAD_REQUEST;
            code = "INVALID_COUPON";
        }
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
                .body(new ApiError(code, exception.getMessage(), List.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedException(Exception exception) {
        LOG.error("Unexpected settlement API failure", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON)
                .body(new ApiError("INTERNAL_ERROR", "服务器处理请求失败", List.of()));
    }
}
