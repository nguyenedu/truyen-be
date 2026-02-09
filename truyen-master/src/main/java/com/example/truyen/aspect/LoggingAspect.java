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

    @Pointcut("execution(* com.example.truyen.service..*(..)) && " +
            "!execution(* com.example.truyen.service.ActivityLogService.*(..))")
    public void serviceLayer() {
    }

    @Around("serviceLayer()")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // Get method info
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = method.getName();
        String fullMethodName = className + "." + methodName;

        // Get parameters
        Object[] args = joinPoint.getArgs();
        String parameters = activityLogService.convertObjectToJson(args);

        // Get user info and IP
        Long userId = getCurrentUserId();
        String ipAddress = getClientIpAddress();

        // Log to console
        log.info(">>> {} - User: {} - IP: {} - Params: {}",
                fullMethodName, userId, ipAddress, parameters);

        try {
            // Execute method
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

            // Only save activity log if it is a significant action
            if (isSignificantAction(methodName, className)) {
                String tableName = extractTableName(className);
                Long recordId = extractRecordId(result, args);
                String description = buildDescription(methodName, parameters, resultJson, executionTime);
                String username = getCurrentUsername();

                // Send activity log via Kafka
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

            // Log error
            log.error("!!! {} - Time: {}ms - Error: {}",
                    fullMethodName, executionTime, e.getMessage());

            // Only save error log if it is a significant action
            if (isSignificantAction(methodName, className)) {
                String tableName = extractTableName(className);
                String description = buildErrorDescription(methodName, parameters, e.getMessage(), executionTime);
                String username = getCurrentUsername();

                // Send error log via Kafka
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

    /**
     * Checks if the method is a significant action that needs logging.
     * Only log important changes: CREATE, UPDATE, DELETE, LOGIN, LOGOUT, REGISTER
     */
    private boolean isSignificantAction(String methodName, String className) {
        String lowerMethodName = methodName.toLowerCase();
        String lowerClassName = className.toLowerCase();

        // DO NOT log read-only operations
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

        // DO NOT log token/blacklist operations
        if (lowerMethodName.contains("blacklist") ||
                lowerMethodName.contains("token") ||
                lowerMethodName.contains("refresh")) {
            return false;
        }

        // DO NOT log authentication checks (except login/logout/register)
        if (lowerClassName.contains("auth") &&
                !lowerMethodName.equals("login") &&
                !lowerMethodName.equals("logout") &&
                !lowerMethodName.equals("register")) {
            return false;
        }

        // MUST log data modification operations
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

        // Default: NO log
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
            // No getName() method
        }
        return null;
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // Check X-Forwarded-For header (if proxy/load balancer)
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
        // e.g.: StoryService -> stories
        // UserService -> users
        if (className.endsWith("Service")) {
            String entityName = className.replace("Service", "");
            return entityName.toLowerCase() + "s";
        }
        return null;
    }

    private Long extractRecordId(Object result, Object[] args) {
        try {
            // If result has getId() method
            if (result != null) {
                Method getIdMethod = result.getClass().getMethod("getId");
                Object id = getIdMethod.invoke(result);
                if (id instanceof Integer) {
                    return (Long) id;
                } else if (id instanceof Long) {
                    return ((Long) id).longValue();
                }
            }

            // Or take from first parameter if it is an ID
            if (args.length > 0 && args[0] instanceof Long) {
                return ((Long) args[0]).longValue();
            } else if (args.length > 0 && args[0] instanceof Integer) {
                return (Long) args[0];
            }
        } catch (Exception e) {
            // Cannot extract ID
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

    // Describe error info
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
}