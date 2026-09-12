package com.shop.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.points.dto.MemberCenterMemberVO;
import com.shop.user.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserMapper extends BaseMapper<User> {

    @Select("""
            <script>
            SELECT u.id, u.id AS user_id, u.nickname, u.avatar, u.phone,
                   mp.status, mp.level, ml.name AS level_name,
                   COALESCE(pa.balance, 0) AS points_balance,
                   COALESCE(pa.total_points, 0) AS total_points,
                   u.created_at AS joined_at, u.last_login_at
            FROM `user` u
            LEFT JOIN member_profile mp ON mp.user_id = u.id
                AND mp.merchant_id = u.merchant_id AND mp.deleted = 0
            LEFT JOIN points_account pa ON pa.user_id = u.id
                AND pa.merchant_id = u.merchant_id AND pa.deleted = 0
            LEFT JOIN member_level ml ON ml.merchant_id = u.merchant_id
                AND ml.level_no = mp.level AND ml.deleted = 0
            WHERE u.merchant_id = #{merchantId} AND u.deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (u.nickname LIKE CONCAT('%', #{keyword}, '%')
                   OR u.phone LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY u.created_at DESC, u.id DESC
            </script>
            """)
    IPage<MemberCenterMemberVO> selectMerchantUserPage(Page<?> page,
                                                        @Param("merchantId") Long merchantId,
                                                        @Param("keyword") String keyword);
}
