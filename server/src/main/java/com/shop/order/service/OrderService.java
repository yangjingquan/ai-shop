package com.shop.order.service;

import com.shop.common.response.PageResult;
import com.shop.order.dto.*;

import java.util.List;

public interface OrderService {

    /** 结算预览：拆单 + 算金额，只读 */
    OrderPreviewVO preview(Long userId, OrderPreviewRequest req);

    /** 下单：库存扣减 + 地址/SKU快照 + 删购物车，返回每个商家一个订单 */
    List<OrderCreateVO> create(Long userId, OrderCreateRequest req);

    /** 用户主动取消（仅 WAIT_PAY） */
    void cancelByUser(Long userId, String orderNo);

    /** 定时任务扫过期订单（status=0 且超过 30 分钟），逐条取消 */
    int cancelExpired(int batchLimit);

    /** 订单分页列表 */
    PageResult<OrderListVO> page(Long userId, int page, int size, Integer status);

    /** 订单详情 */
    OrderDetailVO detail(Long userId, String orderNo);

    /** 商家发货 */
    void ship(Long merchantId, String orderNo, String shipNo);

    /** 用户确认收货 */
    void confirmReceive(Long userId, String orderNo);

    /** 用户申请退款 */
    void refundApply(Long userId, String orderNo, String reason);

    /** 商家审批退款 */
    void refundApprove(Long merchantId, Long refundId, boolean approved, String rejectReason);

    /** 重新支付（仅 WAIT_PAY 且未超时），返新 PayParams */
    OrderCreateVO repay(Long userId, String orderNo);
}
