package com.example.truyen.aspect;

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

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class LoggingAspect {

    private final ActivityLogService activityLogService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Pointcut("execution(* com.example.truyen.service..*(..)) && " +
            "!execution(* com.example.truyen.service.ActivityLogService.*(..))")
    public void serviceLayer() {
    }

    @Around("serviceLayer()")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // Lấy thông tin phương thức
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = method.getName();
        String fullMethodName = className + "." + methodName;

        // Lấy tham số
        Object[] args = joinPoint.getArgs();
        String parameters = convertObjectToJson(args);

        // Lấy thông tin người dùng và IP
        Long userId = getCurrentUserId();
        String ipAddress = getClientIpAddress();

        // Ghi log ra console
        log.info(">>> {} - User: {} - IP: {} - Params: {}",
                fullMethodName, userId, ipAddress, parameters);

        try {
            // Thực thi phương thức
            Object result = joinPoint.proceed();

            long executionTime = System.currentTimeMillis() - startTime;

            // Chuyển đổi kết quả
            String resultJson = convertObjectToJson(result);

            // Ghi log console
            log.info("<<< {} - Time: {}ms - Result: {}",
                    fullMethodName, executionTime,
                    resultJson != null && resultJson.length() > 200
                            ? resultJson.substring(0, 200) + "..."
                            : resultJson);

            // Chỉ lưu nhật ký hoạt động nếu là hành động quan trọng
            if (isSignificantAction(methodName, className)) {
                String tableName = extractTableName(className);
                Long recordId = extractRecordId(result, args);
                String description = buildDescription(methodName, parameters, resultJson, executionTime);
                String username = getCurrentUsername();

                // Gửi nhật ký hoạt động qua Kafka
                activityLogService.logActivity(
                        fullMethodName,
                        tableName,
                        recordId,
                        extractEntityName(result),
                        userId,
                        username,
                        description);
            }

            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            // Ghi log lỗi
            log.error("!!! {} - Time: {}ms - Error: {}",
                    fullMethodName, executionTime, e.getMessage());

            // Chỉ lưu nhật ký lỗi nếu là hành động quan trọng
            if (isSignificantAction(methodName, className)) {
                String tableName = extractTableName(className);
                String description = buildErrorDescription(methodName, parameters, e.getMessage(), executionTime);
                String username = getCurrentUsername();

                // Gửi nhật ký lỗi qua Kafka
                activityLogService.logActivity(
                        fullMethodName + " [ERROR]",
                        tableName,
                        null,
                        null,
                        userId,
                        username,
                        description);
            }

            throw e;
        }
    }

    // Kiểm tra xem phương thức có phải là hành động quan trọng cần ghi log không
    private boolean isSignificantAction(String methodName, String className) {
        String lowerMethodName = methodName.toLowerCase();
        String lowerClassName = className.toLowerCase();

        // Không ghi log các thao tác chỉ đọc
        if (lowerMethodName.startsWith("get") ||
                lowerMethodName.startsWith("find") ||
                lowerMethodName.startsWith("search") ||
                lowerMethodName.startsWith("list") ||
                lowerMethodName.startsWith("check") ||
                lowerMethodName.startsWith("verify") ||
                lowerMethodName.startsWith("validate") ||
                lowerMethodName.startsWith("load") ||
                lowerMethodName.startsWith("fetch")) {
            return false;
        }

        // Không ghi log các thao tác token/blacklist
        if (lowerMethodName.contains("blacklist") ||
                lowerMethodName.contains("token") ||
                lowerMethodName.contains("refresh")) {
            return false;
        }

        // Không ghi log kiểm tra xác thực (ngoại trừ login/logout/register)
        if (lowerClassName.contains("auth") &&
                !lowerMethodName.equals("login") &&
                !lowerMethodName.equals("logout") &&
                !lowerMethodName.equals("register")) {
            return false;
        }

        // Phải ghi log các thao tác sửa đổi dữ liệu
        if (lowerMethodName.startsWith("create") ||
                lowerMethodName.startsWith("add") ||
                lowerMethodName.startsWith("save") ||
                lowerMethodName.startsWith("insert") ||
                lowerMethodName.startsWith("update") ||
                lowerMethodName.startsWith("edit") ||
                lowerMethodName.startsWith("modify") ||
                lowerMethodName.startsWith("change") ||
                lowerMethodName.startsWith("delete") ||
                lowerMethodName.startsWith("remove") ||
                lowerMethodName.equals("login") ||
                lowerMethodName.equals("logout") ||
                lowerMethodName.equals("register")) {
            return true;
        }

        // Mặc định: Không ghi log
        return false;
    }

    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {

                Object principal = authentication.getPrincipal();
                if (principal instanceof User) {
                    return ((User) principal).getId();
                }
            }
        } catch (Exception e) {
            log.warn("Cannot get user info: {}", e.getMessage());
        }
        return null;
    }

    private String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.warn("Cannot get username: {}", e.getMessage());
        }
        return "Anonymous";
    }

    private String extractEntityName(Object result) {
        try {
            if (result != null) {
                Method getNameMethod = result.getClass().getMethod("getName");
                Object name = getNameMethod.invoke(result);
                return name != null ? name.toString() : null;
            }
        } catch (Exception e) {
        }
        return null;
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
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
            log.warn("Cannot get IP address: {}", e.getMessage());
        }
        return "Unknown";
    }

    private String extractTableName(String className) {
        if (className.endsWith("Service")) {
            String entityName = className.replace("Service", "");
            return entityName.toLowerCase() + "s";
        }
        return null;
    }

    private Long extractRecordId(Object result, Object[] args) {
        try {
            if (result != null) {
                Method getIdMethod = result.getClass().getMethod("getId");
                Object id = getIdMethod.invoke(result);
                if (id instanceof Integer) {
                    return (Long) id;
                } else if (id instanceof Long) {
                    return ((Long) id).longValue();
                }
            }
            if (args.length > 0 && args[0] instanceof Long) {
                return ((Long) args[0]).longValue();
            } else if (args.length > 0 && args[0] instanceof Integer) {
                return (Long) args[0];
            }
        } catch (Exception e) {
        }
        return null;
    }

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
                executionTime);
    }

    // Mô tả thông tin lỗi
    private String buildErrorDescription(String methodName, String parameters,
            String errorMessage, long executionTime) {
        return String.format(
                "Method: %s | Params: %s | ERROR: %s | Time: %dms",
                methodName,
                parameters != null && parameters.length() > 500
                        ? parameters.substring(0, 500) + "..."
                        : parameters,
                errorMessage,
                executionTime);
    }

    private String convertObjectToJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }
}