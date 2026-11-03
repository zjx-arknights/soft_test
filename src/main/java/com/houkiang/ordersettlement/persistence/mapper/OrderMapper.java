package com.houkiang.ordersettlement.persistence.mapper;

import com.houkiang.ordersettlement.order.domain.OrderStatus;
import com.houkiang.ordersettlement.persistence.model.OrderCouponRow;
import com.houkiang.ordersettlement.persistence.model.OrderItemRow;
import com.houkiang.ordersettlement.persistence.model.OrderRow;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderMapper {

    @Insert("""
            INSERT INTO settlement_orders (order_id, recipient_name, phone, detail_address, region_type,
                delivery_type, item_amount, discount_amount, shipping_fee, payable_amount, status, stock_reserved)
            VALUES (#{orderId}, #{recipientName}, #{phone}, #{detailAddress}, #{regionType},
                #{deliveryType}, #{itemAmount}, #{discountAmount}, #{shippingFee}, #{payableAmount}, #{status}, TRUE)
            """)
    void insertOrder(OrderRow order);

    @Insert("""
            <script>
            INSERT INTO order_items (order_id, line_number, product_id, product_name, unit_price, stock_snapshot, quantity)
            VALUES
            <foreach collection="items" item="item" separator=",">
                (#{item.orderId}, #{item.lineNumber}, #{item.productId}, #{item.productName},
                 #{item.unitPrice}, #{item.stockSnapshot}, #{item.quantity})
            </foreach>
            </script>
            """)
    void insertItems(@Param("items") List<OrderItemRow> items);

    @Insert("""
            <script>
            INSERT INTO order_coupons (order_id, coupon_position, coupon_code, coupon_type,
                threshold_amount, reduction_amount, discount_rate, stackable)
            VALUES
            <foreach collection="coupons" item="coupon" separator=",">
                (#{coupon.orderId}, #{coupon.couponPosition}, #{coupon.couponCode}, #{coupon.couponType},
                 #{coupon.thresholdAmount}, #{coupon.reductionAmount}, #{coupon.discountRate}, #{coupon.stackable})
            </foreach>
            </script>
            """)
    void insertCoupons(@Param("coupons") List<OrderCouponRow> coupons);

    @Select("""
            SELECT order_id, recipient_name, phone, detail_address, region_type, delivery_type,
                item_amount, discount_amount, shipping_fee, payable_amount, status
            FROM settlement_orders WHERE order_id = #{id}
            """)
    OrderRow findOrder(@Param("id") String orderId);

    @Select("""
            SELECT order_id, line_number, product_id, product_name, unit_price, stock_snapshot, quantity
            FROM order_items WHERE order_id = #{id} ORDER BY line_number
            """)
    List<OrderItemRow> findItems(@Param("id") String orderId);

    @Select("""
            SELECT order_id, coupon_position, coupon_code, coupon_type,
                threshold_amount, reduction_amount, discount_rate, stackable
            FROM order_coupons WHERE order_id = #{id} ORDER BY coupon_position
            """)
    List<OrderCouponRow> findCoupons(@Param("id") String orderId);

    @Update("""
            UPDATE settlement_orders SET status = #{target}, updated_at = CURRENT_TIMESTAMP
            WHERE order_id = #{id} AND status = #{expected}
            """)
    int updateStatus(@Param("id") String orderId, @Param("expected") OrderStatus expectedStatus,
            @Param("target") OrderStatus targetStatus);

    @Select("SELECT stock_reserved FROM settlement_orders WHERE order_id = #{id}")
    boolean wasStockReserved(@Param("id") String orderId);
}
