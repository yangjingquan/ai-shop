package com.shop.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateWxProfileRequest {
    @Size(max = 50, message = "昵称不能超过50个字符")
    private String nickname;

    @Size(max = 1024, message = "头像地址不能超过1024个字符")
    private String avatar;
}
