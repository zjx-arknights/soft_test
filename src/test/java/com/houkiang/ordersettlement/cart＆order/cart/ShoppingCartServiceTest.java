package com.houkiang.ordersettlement.cart.service;

import com.houkiang.ordersettlement.cart.domain.CartItem;
import com.houkiang.ordersettlement.cart.domain.Product;
import com.houkiang.ordersettlement.cart.domain.ShoppingCart;
import com.houkiang.ordersettlement.cart.exception.InvalidPriceException;
import com.houkiang.ordersettlement.cart.exception.InvalidQuantityException;
import com.houkiang.ordersettlement.cart.exception.ProductNotFoundInCartException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ShoppingCartServiceTest {

    private ShoppingCartService service;
    private ShoppingCart cart;
    private Product apple;
    private Product banana;

    @BeforeEach
    void setUp() {
        service = new ShoppingCartService();
        cart = new ShoppingCart();
        apple = new Product("P001", "Apple", new BigDecimal("5.99"), 100);
        banana = new Product("P002", "Banana", new BigDecimal("3.50"), 50);
    }

    // ---------- 正常加入一个商品 ----------
    @Test
    void addItem_ShouldAddNewItem_WhenProductNotInCart() {
        service.addItem(cart, apple, 2);
        assertEquals(1, cart.getItems().size());
        CartItem item = cart.getItems().get(0);
        assertEquals(apple, item.getProduct());
        assertEquals(2, item.getQuantity());
    }

    // ---------- 重复加入同一个商品时数量累加 ----------
    @Test
    void addItem_ShouldAccumulateQuantity_WhenProductAlreadyInCart() {
        service.addItem(cart, apple, 1);
        service.addItem(cart, apple, 3);
        assertEquals(1, cart.getItems().size());
        assertEquals(4, cart.getItems().get(0).getQuantity());
    }

    // ---------- 数量为 0 或负数时抛出 InvalidQuantityException ----------
    @Test
    void addItem_ShouldThrowInvalidQuantityException_WhenQuantityIsZero() {
        assertThrows(InvalidQuantityException.class, () -> service.addItem(cart, apple, 0));
    }

    @Test
    void addItem_ShouldThrowInvalidQuantityException_WhenQuantityIsNegative() {
        assertThrows(InvalidQuantityException.class, () -> service.addItem(cart, apple, -5));
    }

    @Test
    void updateQuantity_ShouldThrowInvalidQuantityException_WhenNewQuantityIsZero() {
        service.addItem(cart, apple, 2);
        assertThrows(InvalidQuantityException.class, () -> service.updateQuantity(cart, apple.getProductId(), 0));
    }

    @Test
    void updateQuantity_ShouldThrowInvalidQuantityException_WhenNewQuantityIsNegative() {
        service.addItem(cart, apple, 2);
        assertThrows(InvalidQuantityException.class, () -> service.updateQuantity(cart, apple.getProductId(), -3));
    }

    // ---------- 商品价格为负数时抛出 InvalidPriceException ----------
    @Test
    void addItem_ShouldThrowInvalidPriceException_WhenProductPriceIsNegative() {
        Product negativePriceProduct = new Product("P003", "Bad", new BigDecimal("-1.00"), 10);
        assertThrows(InvalidPriceException.class, () -> service.addItem(cart, negativePriceProduct, 1));
    }

    // 注意：Product 构造器本身就会校验价格，所以 addItem 中的 validatePrice 会再次校验（幂等）
    @Test
    void productCreation_ShouldThrowInvalidPriceException_WhenPriceIsNegative() {
        assertThrows(InvalidPriceException.class, () -> new Product("P004", "Faulty", new BigDecimal("-2.50"), 5));
    }

    // ---------- 移除不存在的商品时抛出 ProductNotFoundInCartException ----------
    @Test
    void removeItem_ShouldThrowProductNotFoundInCartException_WhenProductNotInCart() {
        assertThrows(ProductNotFoundInCartException.class, () -> service.removeItem(cart, apple.getProductId()));
    }

    // ---------- 修改不存在的商品数量时抛出 ProductNotFoundInCartException ----------
    @Test
    void updateQuantity_ShouldThrowProductNotFoundInCartException_WhenProductNotInCart() {
        assertThrows(ProductNotFoundInCartException.class, () -> service.updateQuantity(cart, apple.getProductId(), 3));
    }

    // ---------- 空购物车总金额为 0 ----------
    @Test
    void calculateCartSubtotal_ShouldReturnZero_WhenCartIsEmpty() {
        BigDecimal subtotal = service.calculateCartSubtotal(cart);
        assertEquals(BigDecimal.ZERO, subtotal);
    }

    // ---------- 额外补充测试（增强覆盖率） ----------

    // 正常移除商品
    @Test
    void removeItem_ShouldRemoveItem_WhenProductExists() {
        service.addItem(cart, apple, 2);
        service.removeItem(cart, apple.getProductId());
        assertTrue(cart.isEmpty());
    }

    // 正常修改数量
    @Test
    void updateQuantity_ShouldUpdateQuantity_WhenProductExists() {
        service.addItem(cart, apple, 2);
        service.updateQuantity(cart, apple.getProductId(), 5);
        CartItem item = cart.getItems().get(0);
        assertEquals(5, item.getQuantity());
    }

    // 单个购物车条目小计计算正确
    @Test
    void calculateItemSubtotal_ShouldReturnCorrectAmount() {
        CartItem item = new CartItem(apple, 3);
        BigDecimal subtotal = service.calculateItemSubtotal(item);
        assertEquals(new BigDecimal("17.97"), subtotal); // 5.99 * 3 = 17.97
    }

    // 总金额计算正确（多商品）
    @Test
    void calculateCartSubtotal_ShouldReturnSumOfAllItemSubtots() {
        service.addItem(cart, apple, 2);   // 5.99 * 2 = 11.98
        service.addItem(cart, banana, 3);  // 3.50 * 3 = 10.50
        BigDecimal total = service.calculateCartSubtotal(cart);
        assertEquals(new BigDecimal("22.48"), total);
    }

    // 测试 addItem 时传入 null 参数
    @Test
    void addItem_ShouldThrowNullPointerException_WhenCartIsNull() {
        assertThrows(NullPointerException.class, () -> service.addItem(null, apple, 1));
    }

    @Test
    void addItem_ShouldThrowNullPointerException_WhenProductIsNull() {
        assertThrows(NullPointerException.class, () -> service.addItem(cart, null, 1));
    }

    // 测试 removeItem 时 productId 为空或空白
    @Test
    void removeItem_ShouldThrowIllegalArgumentException_WhenProductIdIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> service.removeItem(cart, ""));
        assertThrows(IllegalArgumentException.class, () -> service.removeItem(cart, null));
    }

    // 测试 updateQuantity 时 productId 为空或空白
    @Test
    void updateQuantity_ShouldThrowIllegalArgumentException_WhenProductIdIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> service.updateQuantity(cart, "", 1));
        assertThrows(IllegalArgumentException.class, () -> service.updateQuantity(cart, null, 1));
    }
}