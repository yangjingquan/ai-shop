package com.shop.common.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.security.CurrentUser;
import com.shop.common.security.CurrentUserHolder;
import com.shop.oplog.mapper.OpLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Parameter;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OpLogAspect {

    private final OpLogMapper opLogMapper;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_PAYLOAD_BYTES = 10240; // 10KB

    @AfterReturning(pointcut = "@annotation(com.shop.common.aop.OpLog)", returning = "result")
    public void after(JoinPoint joinPoint) {
        OpLog opLog = ((MethodSignature) joinPoint.getSignature()).getMethod()
                .getAnnotation(OpLog.class);
        if (opLog == null) return;

        CurrentUser user = CurrentUserHolder.get();
        if (user == null) return;

        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes()).getRequest();
        String xfwd = request.getHeader("X-Forwarded-For");
        final String ip = (xfwd != null && !xfwd.isBlank()) ? xfwd : request.getRemoteAddr();

        String targetId = resolveTargetId(joinPoint, opLog);
        String payload = safePayload(joinPoint);

        try {
            com.shop.oplog.entity.OpLog entity = new com.shop.oplog.entity.OpLog();
            entity.setOperatorType(mapUserType(user.getUserType().name()));
            entity.setOperatorId(user.getUserId());
            entity.setAction(opLog.action());
            entity.setTargetType(opLog.targetType());
            entity.setTargetId(targetId);
            entity.setPayload(payload);
            entity.setIp(ip);
            opLogMapper.insert(entity);
        } catch (Exception e) {
            log.error("OpLog 落库失败 action={}", opLog.action(), e);
        }
    }

    private String resolveTargetId(JoinPoint joinPoint, OpLog opLog) {
        String expr = opLog.targetIdExpr();
        if (expr == null || expr.isEmpty()) return "";
        // 简单实现：从第 1 个 String 参数取（若 SpEL 以 # 开头则忽略，直接取第 1 个 String 参数）
        Object[] args = joinPoint.getArgs();
        if (args.length > 0) {
            for (Object arg : args) {
                if (arg instanceof String s) return s;
            }
        }
        return "";
    }

    private String safePayload(JoinPoint joinPoint) {
        try {
            MethodSignature sig = (MethodSignature) joinPoint.getSignature();
            Parameter[] params = sig.getMethod().getParameters();
            Object[] args = joinPoint.getArgs();
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            for (int i = 0; i < params.length; i++) {
                String key = params[i].getName();
                Object val = args[i];
                try {
                    // 先序列化再反序列化，避免循环引用 / 不可序列化对象
                    String json = objectMapper.writeValueAsString(val);
                    map.put(key, objectMapper.readTree(json));
                } catch (Exception e) {
                    map.put(key, String.valueOf(val));
                }
            }
            String s = objectMapper.writeValueAsString(map);
            if (s.length() > MAX_PAYLOAD_BYTES) {
                s = s.substring(0, MAX_PAYLOAD_BYTES) + "...";
            }
            return s;
        } catch (Exception e) {
            return "{}";
        }
    }

    private int mapUserType(String type) {
        if ("WX".equals(type)) return 3;
        if ("MERCHANT".equals(type)) return 2;
        if ("ADMIN".equals(type)) return 1;
        return 0;
    }
}
