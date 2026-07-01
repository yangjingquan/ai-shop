package com.shop.common.aop;

import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.security.CurrentUser;
import com.shop.common.security.CurrentUserHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String LUA = """
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local current = redis.call('INCR', key)
            if current == 1 then
                redis.call('EXPIRE', key, window)
            end
            if current > limit then
                return 0
            end
            return 1
            """;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String key = buildKey(rateLimit);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA, Long.class);
        Long result = stringRedisTemplate.execute(script, List.of(key),
                String.valueOf(rateLimit.limit()), String.valueOf(rateLimit.windowSec()));
        if (result == null || result == 0) {
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }
        return pjp.proceed();
    }

    private String buildKey(RateLimit ann) {
        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes()).getRequest();
        String path = request.getRequestURI();
        String resolved;
        if (ann.by() == RateLimit.By.USER) {
            CurrentUser user = CurrentUserHolder.get();
            resolved = String.valueOf(user != null ? user.getUserId() : "anon");
        } else {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
            resolved = ip;
        }
        return "rate:" + ann.key() + ":" + path + ":" + resolved;
    }
}
