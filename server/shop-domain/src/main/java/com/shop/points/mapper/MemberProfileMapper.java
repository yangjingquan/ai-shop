package com.shop.points.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.points.dto.MemberCenterMemberVO;
import com.shop.points.entity.MemberProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MemberProfileMapper extends BaseMapper<MemberProfile> {

    @Select("""
            <script>
            SELECT mp.id, mp.user_id, u.nickname, u.avatar, u.phone, mp.status, mp.level,
                   ml.name AS level_name, COALESCE(pa.balance, 0) AS points_balance,
                   COALESCE(pa.total_points, 0) AS total_points, mp.joined_at
            FROM member_profile mp
            LEFT JOIN `user` u ON u.id = mp.user_id AND u.deleted = 0
            LEFT JOIN points_account pa ON pa.user_id = mp.user_id
                AND pa.merchant_id = mp.merchant_id AND pa.deleted = 0
            LEFT JOIN member_level ml ON ml.merchant_id = mp.merchant_id
                AND ml.level_no = COALESCE(mp.level, 1) AND ml.deleted = 0
            WHERE mp.merchant_id = #{merchantId} AND mp.deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (u.nickname LIKE CONCAT('%', #{keyword}, '%')
                   OR u.phone LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="level != null">AND COALESCE(mp.level, 1) = #{level}</if>
            ORDER BY mp.joined_at DESC, mp.id DESC
            </script>
            """)
    IPage<MemberCenterMemberVO> selectMerchantMemberPage(Page<?> page,
                                                           @Param("merchantId") Long merchantId,
                                                           @Param("keyword") String keyword,
                                                           @Param("level") Integer level);
}
