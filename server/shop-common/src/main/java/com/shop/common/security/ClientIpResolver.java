package com.shop.common.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Arrays;

/** 只接受受控反向代理传递的 X-Forwarded-For，避免客户端伪造限流和审计 IP。 */
@Component
public class ClientIpResolver {

    @Value("${shop.security.trusted-proxies:127.0.0.1,::1}")
    private String trustedProxyCidrs;

    public String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) {
            return remoteAddr;
        }
        String first = forwarded.split(",", 2)[0].trim();
        return first.isBlank() ? remoteAddr : first;
    }

    private boolean isTrustedProxy(String address) {
        if (address == null || address.isBlank()) return false;
        return Arrays.stream(trustedProxyCidrs.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .anyMatch(cidr -> matchesCidr(address, cidr));
    }

    private boolean matchesCidr(String address, String cidr) {
        try {
            String[] parts = cidr.split("/", 2);
            InetAddress ip = InetAddress.getByName(address);
            InetAddress network = InetAddress.getByName(parts[0]);
            if (ip.getAddress().length != network.getAddress().length) return false;
            int prefix = parts.length == 1 ? network.getAddress().length * 8 : Integer.parseInt(parts[1]);
            byte[] ipBytes = ip.getAddress();
            byte[] networkBytes = network.getAddress();
            int fullBytes = prefix / 8;
            int remainingBits = prefix % 8;
            for (int i = 0; i < fullBytes; i++) {
                if (ipBytes[i] != networkBytes[i]) return false;
            }
            if (remainingBits == 0) return true;
            int mask = 0xFF << (8 - remainingBits);
            return (ipBytes[fullBytes] & mask) == (networkBytes[fullBytes] & mask);
        } catch (Exception ignored) {
            return false;
        }
    }
}
