package com.houkiang.ordersettlement.test_common_shipping.shipping;

import com.houkiang.ordersettlement.shipping.domain.Address;
import com.houkiang.ordersettlement.shipping.domain.DeliveryType;
import com.houkiang.ordersettlement.shipping.domain.RegionType;
import com.houkiang.ordersettlement.shipping.domain.ShippingCalculationResult;
import com.houkiang.ordersettlement.shipping.domain.ShippingFeeRule;
import com.houkiang.ordersettlement.shipping.exception.InvalidAddressException;
import com.houkiang.ordersettlement.shipping.exception.InvalidShippingRuleException;
import com.houkiang.ordersettlement.shipping.exception.UnsupportedDeliveryTypeException;
import com.houkiang.ordersettlement.shipping.service.ShippingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ShippingService 物流配送服务测试")
class ShippingServiceTest {

    private ShippingService service;

    /** 构造一个字段完整的普通地区地址 */
    private Address normalAddress() {
        return new Address("张三", "13800138000", "北京市朝阳区XX路1号", RegionType.NORMAL);
    }

    /** 构造一个字段完整的偏远地区地址 */
    private Address remoteAddress() {
        return new Address("李四", "13900139000", "西藏自治区XX县XX乡", RegionType.REMOTE);
    }

    @BeforeEach
    void setUp() {
        service = new ShippingService();
    }

    // ─────────────────────────────────────────────────────────────
    // A. validateAddress
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("A. validateAddress 地址校验")
    class ValidateAddress {

        @Test
        @DisplayName("地址为 null → InvalidAddressException")
        void nullAddress_throws() {
            assertThrows(InvalidAddressException.class,
                    () -> service.validateAddress(null));
        }

        @Test
        @DisplayName("收件人为空字符串 → InvalidAddressException")
        void emptyRecipient_throws() {
            Address addr = new Address("", "13800138000", "详细地址", RegionType.NORMAL);
            assertThrows(InvalidAddressException.class,
                    () -> service.validateAddress(addr));
        }

        @Test
        @DisplayName("收件人为纯空白 → InvalidAddressException")
        void blankRecipient_throws() {
            Address addr = new Address("   ", "13800138000", "详细地址", RegionType.NORMAL);
            assertThrows(InvalidAddressException.class,
                    () -> service.validateAddress(addr));
        }

        @Test
        @DisplayName("电话为空字符串 → InvalidAddressException")
        void emptyPhone_throws() {
            Address addr = new Address("张三", "", "详细地址", RegionType.NORMAL);
            assertThrows(InvalidAddressException.class,
                    () -> service.validateAddress(addr));
        }

        @Test
        @DisplayName("详细地址为空字符串 → InvalidAddressException")
        void emptyDetailAddress_throws() {
            Address addr = new Address("张三", "13800138000", "", RegionType.NORMAL);
            assertThrows(InvalidAddressException.class,
                    () -> service.validateAddress(addr));
        }

        @Test
        @DisplayName("regionType 为 null → InvalidAddressException")
        void nullRegionType_throws() {
            Address addr = new Address("张三", "13800138000", "详细地址", null);
            assertThrows(InvalidAddressException.class,
                    () -> service.validateAddress(addr));
        }

        @Test
        @DisplayName("完整合法地址 → 不抛异常")
        void validAddress_noException() {
            assertDoesNotThrow(() -> service.validateAddress(normalAddress()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // B. validateDeliveryType
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("B. validateDeliveryType 配送方式校验")
    class ValidateDeliveryType {

        @Test
        @DisplayName("deliveryType 为 null → NullPointerException")
        void nullDeliveryType_throws() {
            assertThrows(NullPointerException.class,
                    () -> service.validateDeliveryType(null, RegionType.NORMAL));
        }

        @Test
        @DisplayName("regionType 为 null → NullPointerException")
        void nullRegionType_throws() {
            assertThrows(NullPointerException.class,
                    () -> service.validateDeliveryType(DeliveryType.STANDARD, null));
        }

        @Test
        @DisplayName("EXPRESS + REMOTE → UnsupportedDeliveryTypeException")
        void expressRemote_throws() {
            assertThrows(UnsupportedDeliveryTypeException.class,
                    () -> service.validateDeliveryType(DeliveryType.EXPRESS, RegionType.REMOTE));
        }

        @Test
        @DisplayName("STANDARD + NORMAL → 不抛异常")
        void standardNormal_noException() {
            assertDoesNotThrow(() -> service.validateDeliveryType(DeliveryType.STANDARD, RegionType.NORMAL));
        }

        @Test
        @DisplayName("STANDARD + REMOTE → 不抛异常")
        void standardRemote_noException() {
            assertDoesNotThrow(() -> service.validateDeliveryType(DeliveryType.STANDARD, RegionType.REMOTE));
        }

        @Test
        @DisplayName("EXPRESS + NORMAL → 不抛异常")
        void expressNormal_noException() {
            assertDoesNotThrow(() -> service.validateDeliveryType(DeliveryType.EXPRESS, RegionType.NORMAL));
        }

        @Test
        @DisplayName("SELF_PICKUP + NORMAL → 不抛异常")
        void selfPickupNormal_noException() {
            assertDoesNotThrow(() -> service.validateDeliveryType(DeliveryType.SELF_PICKUP, RegionType.NORMAL));
        }

        @Test
        @DisplayName("SELF_PICKUP + REMOTE → 不抛异常")
        void selfPickupRemote_noException() {
            assertDoesNotThrow(() -> service.validateDeliveryType(DeliveryType.SELF_PICKUP, RegionType.REMOTE));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // C. calculateShippingFee — 标准配送 + 普通地区
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("C1. 标准配送 + 普通地区（包邮阈值 88）")
    class StandardNormal {

        @Test
        @DisplayName("87.99 → 运费 8.00，未包邮")
        void below_threshold_chargesFee() {
            ShippingCalculationResult result = service.calculateShippingFee(
                    new BigDecimal("87.99"), normalAddress(), DeliveryType.STANDARD);
            assertEquals(new BigDecimal("8.00"), result.getShippingFee());
            assertFalse(result.isFreeShippingApplied());
        }

        @Test
        @DisplayName("88.00（边界）→ 运费 0.00，已包邮")
        void at_threshold_freeShipping() {
            ShippingCalculationResult result = service.calculateShippingFee(
                    new BigDecimal("88.00"), normalAddress(), DeliveryType.STANDARD);
            assertEquals(new BigDecimal("0.00"), result.getShippingFee());
            assertTrue(result.isFreeShippingApplied());
        }

        @Test
        @DisplayName("88.01 → 运费 0.00，已包邮")
        void above_threshold_freeShipping() {
            ShippingCalculationResult result = service.calculateShippingFee(
                    new BigDecimal("88.01"), normalAddress(), DeliveryType.STANDARD);
            assertEquals(new BigDecimal("0.00"), result.getShippingFee());
            assertTrue(result.isFreeShippingApplied());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // C2. 标准配送 + 偏远地区
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("C2. 标准配送 + 偏远地区（不包邮）")
    class StandardRemote {

        @Test
        @DisplayName("金额 0 → 运费 15.00，未包邮")
        void zero_amount_chargesFee() {
            ShippingCalculationResult result = service.calculateShippingFee(
                    BigDecimal.ZERO, remoteAddress(), DeliveryType.STANDARD);
            assertEquals(new BigDecimal("15.00"), result.getShippingFee());
            assertFalse(result.isFreeShippingApplied());
        }

        @Test
        @DisplayName("金额 200 → 运费仍为 15.00，未包邮")
        void large_amount_stillChargesFee() {
            ShippingCalculationResult result = service.calculateShippingFee(
                    new BigDecimal("200.00"), remoteAddress(), DeliveryType.STANDARD);
            assertEquals(new BigDecimal("15.00"), result.getShippingFee());
            assertFalse(result.isFreeShippingApplied());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // C3. 加急配送 + 普通地区
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("C3. 加急配送 + 普通地区（包邮阈值 188）")
    class ExpressNormal {

        @Test
        @DisplayName("187.99 → 运费 15.00，未包邮")
        void below_threshold_chargesFee() {
            ShippingCalculationResult result = service.calculateShippingFee(
                    new BigDecimal("187.99"), normalAddress(), DeliveryType.EXPRESS);
            assertEquals(new BigDecimal("15.00"), result.getShippingFee());
            assertFalse(result.isFreeShippingApplied());
        }

        @Test
        @DisplayName("188.00（边界）→ 运费 0.00，已包邮")
        void at_threshold_freeShipping() {
            ShippingCalculationResult result = service.calculateShippingFee(
                    new BigDecimal("188.00"), normalAddress(), DeliveryType.EXPRESS);
            assertEquals(new BigDecimal("0.00"), result.getShippingFee());
            assertTrue(result.isFreeShippingApplied());
        }

        @Test
        @DisplayName("188.01 → 运费 0.00，已包邮")
        void above_threshold_freeShipping() {
            ShippingCalculationResult result = service.calculateShippingFee(
                    new BigDecimal("188.01"), normalAddress(), DeliveryType.EXPRESS);
            assertEquals(new BigDecimal("0.00"), result.getShippingFee());
            assertTrue(result.isFreeShippingApplied());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // C4. 加急配送 + 偏远地区
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("C4. 加急配送 + 偏远地区（不支持）")
    class ExpressRemote {

        @Test
        @DisplayName("任意金额 → UnsupportedDeliveryTypeException")
        void expressRemote_throws() {
            assertThrows(UnsupportedDeliveryTypeException.class,
                    () -> service.calculateShippingFee(
                            new BigDecimal("100.00"), remoteAddress(), DeliveryType.EXPRESS));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // C5. 自提配送
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("C5. 自提配送（运费始终 0）")
    class SelfPickup {

        @Test
        @DisplayName("SELF_PICKUP + NORMAL → 运费 0.00，已包邮")
        void selfPickupNormal_freeShipping() {
            ShippingCalculationResult result = service.calculateShippingFee(
                    new BigDecimal("10.00"), normalAddress(), DeliveryType.SELF_PICKUP);
            assertEquals(new BigDecimal("0.00"), result.getShippingFee());
            assertTrue(result.isFreeShippingApplied());
        }

        @Test
        @DisplayName("SELF_PICKUP + REMOTE → 运费 0.00，已包邮")
        void selfPickupRemote_freeShipping() {
            ShippingCalculationResult result = service.calculateShippingFee(
                    new BigDecimal("10.00"), remoteAddress(), DeliveryType.SELF_PICKUP);
            assertEquals(new BigDecimal("0.00"), result.getShippingFee());
            assertTrue(result.isFreeShippingApplied());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // C6. itemAmount 输入边界
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("C6. itemAmount 参数边界")
    class ItemAmountBoundary {

        @Test
        @DisplayName("itemAmount 为负数 → IllegalArgumentException")
        void negativeAmount_throws() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.calculateShippingFee(
                            new BigDecimal("-1.00"), normalAddress(), DeliveryType.STANDARD));
        }

        @Test
        @DisplayName("itemAmount 为 null → NullPointerException")
        void nullAmount_throws() {
            assertThrows(NullPointerException.class,
                    () -> service.calculateShippingFee(
                            null, normalAddress(), DeliveryType.STANDARD));
        }

        @Test
        @DisplayName("itemAmount 为 0 → 正常收费（标准/普通地区收 8.00）")
        void zeroAmount_chargesFee() {
            ShippingCalculationResult result = service.calculateShippingFee(
                    BigDecimal.ZERO, normalAddress(), DeliveryType.STANDARD);
            assertEquals(new BigDecimal("8.00"), result.getShippingFee());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // D. 自定义规则校验（InvalidShippingRuleException）
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("D. 自定义规则异常场景")
    class CustomRuleValidation {

        @Test
        @DisplayName("baseFee 为负数 → InvalidShippingRuleException")
        void negativeBaseFee_throws() {
            Map<String, ShippingFeeRule> badRules = new HashMap<>();
            badRules.put(
                    ShippingService.key(DeliveryType.STANDARD, RegionType.NORMAL),
                    new ShippingFeeRule(DeliveryType.STANDARD, RegionType.NORMAL,
                            new BigDecimal("-1.00"), null));
            ShippingService badService = new ShippingService(badRules);

            assertThrows(InvalidShippingRuleException.class,
                    () -> badService.calculateShippingFee(
                            new BigDecimal("50.00"), normalAddress(), DeliveryType.STANDARD));
        }

        @Test
        @DisplayName("freeShippingThreshold 为负数 → InvalidShippingRuleException")
        void negativeThreshold_throws() {
            Map<String, ShippingFeeRule> badRules = new HashMap<>();
            badRules.put(
                    ShippingService.key(DeliveryType.STANDARD, RegionType.NORMAL),
                    new ShippingFeeRule(DeliveryType.STANDARD, RegionType.NORMAL,
                            new BigDecimal("8.00"), new BigDecimal("-1.00")));
            ShippingService badService = new ShippingService(badRules);

            assertThrows(InvalidShippingRuleException.class,
                    () -> badService.calculateShippingFee(
                            new BigDecimal("50.00"), normalAddress(), DeliveryType.STANDARD));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // E. key() 静态方法
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("E. key() 辅助方法")
    class KeyMethod {

        @Test
        @DisplayName("STANDARD + NORMAL → \"STANDARD:NORMAL\"")
        void standardNormalKey() {
            assertEquals("STANDARD:NORMAL",
                    ShippingService.key(DeliveryType.STANDARD, RegionType.NORMAL));
        }

        @Test
        @DisplayName("EXPRESS + REMOTE → \"EXPRESS:REMOTE\"")
        void expressRemoteKey() {
            assertEquals("EXPRESS:REMOTE",
                    ShippingService.key(DeliveryType.EXPRESS, RegionType.REMOTE));
        }

        @Test
        @DisplayName("SELF_PICKUP + NORMAL → \"SELF_PICKUP:NORMAL\"")
        void selfPickupNormalKey() {
            assertEquals("SELF_PICKUP:NORMAL",
                    ShippingService.key(DeliveryType.SELF_PICKUP, RegionType.NORMAL));
        }
    }
}
