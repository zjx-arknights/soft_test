package com.houkiang.ordersettlement.persistence.mapper;

import com.houkiang.ordersettlement.order.repository.IdempotencyRepository.Claim;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface IdempotencyMapper {
    @Insert("""
            INSERT INTO idempotency_requests (request_key, request_fingerprint)
            VALUES (#{key}, #{fingerprint})
            """)
    void claim(@Param("key") String key, @Param("fingerprint") String fingerprint);

    @Update("""
            UPDATE idempotency_requests SET order_id = #{orderId}
            WHERE request_key = #{key} AND request_fingerprint = #{fingerprint} AND order_id IS NULL
            """)
    int complete(@Param("key") String key, @Param("fingerprint") String fingerprint,
            @Param("orderId") String orderId);

    @Select("""
            SELECT request_key, request_fingerprint, order_id
            FROM idempotency_requests WHERE request_key = #{key}
            """)
    Claim findByKey(@Param("key") String key);
}
