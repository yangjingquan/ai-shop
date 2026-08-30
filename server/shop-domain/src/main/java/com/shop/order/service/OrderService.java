package com.shop.order.service;

import com.shop.common.response.PageResult;
import com.shop.order.dto.*;

import java.util.List;

public interface OrderService {

    /** 结算预览：单个商家内计算金额，只读；跨商家商品直接拒绝 */
    OrderPreviewVO preview(Long userId, OrderPreviewRequest req);

    OrderPreviewVO preview(Long userId, Long merchantId, OrderPreviewRequest req);

    /** 下单并完成微信 JSAPI 预下单：单个商家订单的库存扣减、快照和支付参数 */
    List<OrderCreateVO> create(Long userId, OrderCreateRequest req);

    List<OrderCreateVO> create(Long userId, Long merchantId, OrderCreateRequest req);

    /** 用户主动取消（仅 WAIT_PAY） */
    void cancelByUser(Long userId, String orderNo);

    /** 定时任务扫过期订单（status=0 且超过 30 分钟），逐条取消 */
    int cancelExpired(int batchLimit);

    /** 订单分页列表 */
    PageResult<OrderListVO> page(Long userId, int page, int size, Integer status);

    /** 订单详情 */
    OrderDetailVO detail(Long userId, String orderNo);

    /** 商家订单详情 */
    OrderDetailVO merchantDetail(Long merchantId, String orderNo);

    /** 运营端订单详情，不受商家范围限制，仅供管理员查询。 */
    OrderDetailVO adminDetail(String orderNo);

    /** 商家发货 */
    void ship(Long merchantId, String orderNo, String shipNo);

    /** 商家发货（带物流公司） */
    void ship(Long merchantId, String orderNo, String shipCompany, String shipNo);

    /** 用户确认收货 */
    void confirmReceive(Long userId, String orderNo);

    /** 用户提醒商家发货，短时间内限频。 */
    void remindShip(Long userId, String orderNo);

    /** 用户申请退款 */
    void refundApply(Long userId, String orderNo, String reason);

    void refundApply(Long userId, String orderNo, RefundApplyRequest req);

    /** 商家审批退款 */
    void refundApprove(Long merchantId, Long refundId, boolean approved, String rejectReason);

    /** 重新支付（仅 WAIT_PAY 且未超时），返新 PayParams */
    OrderCreateVO repay(Long userId, String orderNo);

    OrderCreateVO repay(Long userId, Long merchantId, String orderNo);
}
