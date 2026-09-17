package com.kanhacollection.backend.cart;

import com.kanhacollection.backend.cart.dto.AddToCartRequest;
import com.kanhacollection.backend.cart.dto.CartResponse;
import com.kanhacollection.backend.exception.InsufficientStockException;
import com.kanhacollection.backend.inventory.Inventory;
import com.kanhacollection.backend.product.Product;
import com.kanhacollection.backend.product.ProductVariant;
import com.kanhacollection.backend.product.ProductVariantRepository;
import com.kanhacollection.backend.user.User;
import com.kanhacollection.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductVariantRepository variantRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private UUID userId;
    private UUID variantId;
    private User sampleUser;
    private Product sampleProduct;
    private ProductVariant sampleVariant;
    private Cart sampleCart;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        variantId = UUID.randomUUID();

        sampleUser = User.builder().id(userId).email("test@example.com").build();

        sampleProduct = Product.builder()
                .id(UUID.randomUUID())
                .title("Laddu Gopal Dress")
                .slug("laddu-gopal-dress")
                .isActive(true)
                .build();

        Inventory inventory = Inventory.builder()
                .availableQuantity(10)
                .reservedQuantity(0)
                .build();

        sampleVariant = ProductVariant.builder()
                .id(variantId)
                .product(sampleProduct)
                .price(BigDecimal.valueOf(500))
                .isActive(true)
                .inventory(inventory)
                .sku("DRESS-RED-01")
                .build();

        sampleCart = Cart.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .items(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Should get existing cart for user")
    void getCartForUser_Success() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCart));

        CartResponse response = cartService.getCartForUser(userId);

        assertThat(response).isNotNull();
        assertThat(response.getCartId()).isEqualTo(sampleCart.getId());
        assertThat(response.getItems()).isEmpty();
    }

    @Test
    @DisplayName("Should add new item to cart successfully")
    void addItemToCart_NewItem_Success() {
        AddToCartRequest request = AddToCartRequest.builder()
                .variantId(variantId)
                .quantity(2)
                .build();

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCart));
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(sampleVariant));
        when(cartItemRepository.findByCartIdAndVariantId(sampleCart.getId(), variantId)).thenReturn(Optional.empty());

        CartResponse response = cartService.addItemToCart(userId, request);

        assertThat(response).isNotNull();
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
    }

    @Test
    @DisplayName("Should throw InsufficientStockException when requested quantity exceeds available stock")
    void addItemToCart_InsufficientStock_ThrowsException() {
        AddToCartRequest request = AddToCartRequest.builder()
                .variantId(variantId)
                .quantity(15) // Stock is 10
                .build();

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCart));
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(sampleVariant));

        assertThatThrownBy(() -> cartService.addItemToCart(userId, request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Only 10 units available");
    }

    @Test
    @DisplayName("Should remove item from cart")
    void removeItemFromCart_Success() {
        UUID cartItemId = UUID.randomUUID();
        CartItem cartItem = CartItem.builder()
                .id(cartItemId)
                .cart(sampleCart)
                .variant(sampleVariant)
                .quantity(1)
                .build();
        sampleCart.getItems().add(cartItem);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCart));
        when(cartItemRepository.findById(cartItemId)).thenReturn(Optional.of(cartItem));

        CartResponse response = cartService.removeItemFromCart(userId, cartItemId);

        assertThat(response).isNotNull();
        verify(cartItemRepository, times(1)).delete(cartItem);
    }

    @Test
    @DisplayName("Should clear all items from user cart")
    void clearCart_Success() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(sampleCart));

        cartService.clearCart(userId);

        verify(cartItemRepository, times(1)).deleteByCartId(sampleCart.getId());
        assertThat(sampleCart.getItems()).isEmpty();
    }
}
