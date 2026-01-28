package com.example.truyen.aspect;

import com.example.truyen.entity.ActivityLog;
import com.example.truyen.entity.User;
import com.example.truyen.service.ActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class LoggingAspect {

    private final ActivityLogService activityLogService;

    /**
     * Pointcut cho tất cả Service methods
     */

    @Pointcut       ("execution(* com.example.truyen.service..*(..)) && " +
            "!execution(* com.example.truyen.service.ActivityLogService.*(..))")
    public void serviceLayer() {}

    @Around("serviceLayer()")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // Lấy thông tin method
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = method.getName();
        String fullMethodName = className + "." + methodName;

        // Lấy parameters
        Object[] args = joinPoint.getArgs();
        String parameters = activityLogService.convertObjectToJson(args);

        // Lấy thông tin user và IP
        Long userId = getCurrentUserId();
        String ipAddress = getClientIpAddress();

        // Log ra console
        log.info(">>> {} - User: {} - IP: {} - Params: {}",
                fullMethodName, userId, ipAddress, parameters);

        try {
            // Thực thi method
            Object result = joinPoint.proceed();

            long executionTime = System.currentTimeMillis() - startTime;

            // Convert result
            String resultJson = activityLogService.convertObjectToJson(result);

            // Log console
            log.info("<<< {} - Time: {}ms - Result: {}",
                    fullMethodName, executionTime,
                    resultJson != null && resultJson.length() > 200
                            ? resultJson.substring(0, 200) + "..."
                            : resultJson);

            // Tạo activity log
            ActivityLog activityLog = ActivityLog.builder()
                    .userId(userId)
                    .action(fullMethodName)
                    .tableName(extractTableName(className))
                    .recordId(extractRecordId(result, args))
                    .description(buildDescription(methodName, parameters, resultJson, executionTime))
                    .ipAddress(ipAddress)
                    .createdAt(LocalDateTime.now())
                    .build();

            // Lưu vào database
            activityLogService.saveLog(activityLog);

            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            // Log lỗi
            log.error("!!! {} - Time: {}ms - Error: {}",
                    fullMethodName, executionTime, e.getMessage());

            // Lưu log lỗi
            ActivityLog activityLog = ActivityLog.builder()
                    .userId(userId)
                    .action(fullMethodName)
                    .tableName(extractTableName(className))
                    .recordId(null)
                    .description(buildErrorDescription(methodName, parameters, e.getMessage(), executionTime))
                    .ipAddress(ipAddress)
                    .createdAt(LocalDateTime.now())
                    .build();

            activityLogService.saveLog(activityLog);

            throw e;
        }
    }

    /**
     * Lấy User ID từ Spring Security Context
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {

                String username = authentication.getName();

                // Inject UserRepository vào LoggingAspect và query
                // Hoặc cache userId trong custom UserDetails

                return null;
            }
        } catch (Exception e) {
            log.warn("Không lấy được user info: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Lấy IP address của client
     */
    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // Kiểm tra X-Forwarded-For header (nếu có proxy/load balancer)
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }

                String xRealIp = request.getHeader("X-Real-IP");
                if (xRealIp != null && !xRealIp.isEmpty()) {
                    return xRealIp;
                }

                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.warn("Không lấy được IP address: {}", e.getMessage());
        }
        return "Unknown";
    }

    /**
     * Trích xuất table name từ class name
     */
    private String extractTableName(String className) {
        // VD: StoryService -> stories
        // UserService -> users
        if (className.endsWith("Service")) {
            String entityName = className.replace("Service", "");
            return entityName.toLowerCase() + "s";
        }
        return null;
    }

    /**
     * Trích xuất record ID từ result hoặc args
     */
    private Long extractRecordId(Object result, Object[] args) {
        try {
            // Nếu result có method getId()
            if (result != null) {
                Method getIdMethod = result.getClass().getMethod("getId");
                Object id = getIdMethod.invoke(result);
                if (id instanceof Integer) {
                    return (Long) id;
                } else if (id instanceof Long) {
                    return ((Long) id).longValue();
                }
            }

            // Hoặc lấy từ parameter đầu tiên nếu là ID
            if (args.length > 0 && args[0] instanceof Long) {
                return ((Long) args[0]).longValue();
            } else if (args.length > 0 && args[0] instanceof Integer) {
                return (Long) args[0];
            }
        } catch (Exception e) {
            // Không extract được ID
        }
        return null;
    }

    /**
     * Build description cho success case
     */
    private String buildDescription(String methodName, String parameters,
                                    String result, long executionTime) {
        return String.format(
                "Method: %s | Params: %s | Result: %s | Time: %dms",
                methodName,
                parameters != null && parameters.length() > 500
                        ? parameters.substring(0, 500) + "..."
                        : parameters,
                result != null && result.length() > 500
                        ? result.substring(0, 500) + "..."
                        : result,
                executionTime
        );
    }

    //Miêu tả thông tin lỗi
    private String buildErrorDescription(String methodName, String parameters,
                                         String errorMessage, long executionTime) {
        return String.format(
                "Method: %s | Params: %s | ERROR: %s | Time: %dms",
                methodName,
                parameters != null && parameters.length() > 500
                        ? parameters.substring(0, 500) + "..."
                        : parameters,
                errorMessage,
                executionTime
        );
    }
}