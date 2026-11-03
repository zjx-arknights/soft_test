package com.houkiang.ordersettlement.persistence.mapper;

import com.houkiang.ordersettlement.cart.domain.Product;
import com.houkiang.ordersettlement.promotion.domain.Coupon;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CatalogMapper {

    @Select("SELECT product_id, product_name, unit_price, stock FROM products WHERE product_id = #{id}")
    Product findProduct(@Param("id") String productId);

    @Select("SELECT product_id, product_name, unit_price, stock FROM products ORDER BY product_id")
    List<Product> listProducts();

    @Select("""
            SELECT coupon_code, coupon_type, threshold_amount, reduction_amount, discount_rate, stackable
            FROM coupons WHERE coupon_code = #{code}
            """)
    Coupon findCoupon(@Param("code") String couponCode);

    @Select("""
            SELECT coupon_code, coupon_type, threshold_amount, reduction_amount, discount_rate, stackable
            FROM coupons ORDER BY coupon_code
            """)
    List<Coupon> listCoupons();

    @Update("""
            UPDATE products SET stock = stock - #{quantity}
            WHERE product_id = #{id} AND stock >= #{quantity}
            """)
    int reserveStock(@Param("id") String productId, @Param("quantity") int quantity);

    @Update("UPDATE products SET stock = stock + #{quantity} WHERE product_id = #{id}")
    int releaseStock(@Param("id") String productId, @Param("quantity") int quantity);
}
