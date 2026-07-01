package com.shop.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressSnapshot {

    private String receiver;

    private String phone;

    private String region;

    private String detail;
}
