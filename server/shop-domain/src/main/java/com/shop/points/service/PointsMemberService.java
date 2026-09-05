package com.shop.points.service;
import com.shop.order.entity.Order;
import com.shop.points.dto.*;
import java.math.BigDecimal;
import java.util.List;
public interface PointsMemberService {
 void registerMember(Long userId, Long merchantId);
 PointsProfileVO profile(Long userId, Long merchantId);
 List<PointsLedgerVO> ledger(Long userId, Long merchantId, int limit);
 PointsProfileVO signIn(Long userId, Long merchantId);
 List<PointsProductVO> mall(Long userId, Long merchantId);
 PointsRedeemVO redeem(Long userId, Long merchantId, PointsRedeemRequest request);
 MemberDayActivityVO memberDay(Long userId, Long merchantId);
 Long receiveMemberDayCoupon(Long userId, Long merchantId);
 PointsRuleRequest rule(Long merchantId); void saveRule(Long merchantId, PointsRuleRequest request);
 List<PointsProductVO> merchantProducts(Long merchantId); Long saveProduct(Long merchantId, Long id, PointsProductRequest request); void deleteProduct(Long merchantId, Long id);
 MemberDayActivityVO merchantMemberDay(Long merchantId); void saveMemberDay(Long merchantId, MemberDayActivityRequest request);
 void rewardPaidOrder(Order order); void reverseRefund(Long merchantId, Long userId, String refundNo, BigDecimal amount, String orderNo);
}
