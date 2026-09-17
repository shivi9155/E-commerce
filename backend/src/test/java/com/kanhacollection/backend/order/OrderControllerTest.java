package com.kanhacollection.backend.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanhacollection.backend.auth.CustomUserDetailsService;
import com.kanhacollection.backend.auth.JwtTokenProvider;
import com.kanhacollection.backend.auth.UserPrincipal;
import com.kanhacollection.backend.common.PagedResponse;
import com.kanhacollection.backend.order.dto.CreateOrderRequest;
import com.kanhacollection.backend.order.dto.OrderAddressRequest;
import com.kanhacollection.backend.order.dto.OrderResponse;
import com.kanhacollection.backend.user.Role;
import com.kanhacollection.backend.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("POST /orders/checkout - Should checkout cart and create order")
    void createOrder_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        User userEntity = User.builder()
                .id(userId)
                .email("test@example.com")
                .role(Role.ROLE_USER)
                .enabled(true)
                .build();
        UserPrincipal principal = UserPrincipal.create(userEntity);

        OrderAddressRequest addressRequest = OrderAddressRequest.builder()
                .recipientName("Test User")
                .phone("9876543210")
                .addressLine1("123 Temple Street")
                .city("Vrindavan")
                .state("Uttar Pradesh")
                .pincode("281121")
                .country("India")
                .build();

        CreateOrderRequest request = CreateOrderRequest.builder()
                .shippingAddress(addressRequest)
                .build();

        OrderResponse orderResponse = OrderResponse.builder()
                .id(UUID.randomUUID())
                .orderNumber("ORD-123456")
                .orderStatus("PENDING_PAYMENT")
                .paymentStatus("PENDING")
                .netAmount(BigDecimal.valueOf(1499.00))
                .build();

        when(orderService.createOrderFromCart(eq(userId), any(CreateOrderRequest.class))).thenReturn(orderResponse);

        mockMvc.perform(post("/orders/checkout")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-123456"))
                .andExpect(jsonPath("$.data.orderStatus").value("PENDING_PAYMENT"));
    }

    @Test
    @DisplayName("GET /orders/me - Should return current user order history")
    void getUserOrders_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        User userEntity = User.builder()
                .id(userId)
                .email("test@example.com")
                .role(Role.ROLE_USER)
                .enabled(true)
                .build();
        UserPrincipal principal = UserPrincipal.create(userEntity);

        OrderResponse orderResponse = OrderResponse.builder()
                .id(UUID.randomUUID())
                .orderNumber("ORD-123456")
                .orderStatus("DELIVERED")
                .paymentStatus("PAID")
                .netAmount(BigDecimal.valueOf(1499.00))
                .build();

        PagedResponse<OrderResponse> pagedResponse = PagedResponse.<OrderResponse>builder()
                .content(Collections.singletonList(orderResponse))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .isLast(true)
                .build();

        when(orderService.getUserOrders(eq(userId), eq(0), eq(10))).thenReturn(pagedResponse);

        mockMvc.perform(get("/orders/me")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].orderNumber").value("ORD-123456"));
    }
}
