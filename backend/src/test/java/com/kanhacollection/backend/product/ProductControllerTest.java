package com.kanhacollection.backend.product;

import com.kanhacollection.backend.auth.CustomUserDetailsService;
import com.kanhacollection.backend.auth.JwtTokenProvider;
import com.kanhacollection.backend.common.PagedResponse;
import com.kanhacollection.backend.product.dto.ProductFilterRequest;
import com.kanhacollection.backend.product.dto.ProductResponse;
import com.kanhacollection.backend.product.dto.ProductSummaryResponse;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("GET /products - Should return paginated products")
    void searchAndFilterProducts_Success() throws Exception {
        ProductSummaryResponse summary = ProductSummaryResponse.builder()
                .id(UUID.randomUUID())
                .title("Brass Kanha Idol")
                .slug("brass-kanha-idol")
                .minPrice(BigDecimal.valueOf(999))
                .maxPrice(BigDecimal.valueOf(1499))
                .inStock(true)
                .build();

        PagedResponse<ProductSummaryResponse> pagedResponse = PagedResponse.<ProductSummaryResponse>builder()
                .content(Collections.singletonList(summary))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .isLast(true)
                .build();

        when(productService.searchAndFilterProducts(any(ProductFilterRequest.class))).thenReturn(pagedResponse);

        mockMvc.perform(get("/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value("Brass Kanha Idol"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /products/{slug} - Should return product detail by slug")
    void getProductBySlug_Success() throws Exception {
        ProductResponse response = ProductResponse.builder()
                .id(UUID.randomUUID())
                .title("Brass Kanha Idol")
                .slug("brass-kanha-idol")
                .description("Handcrafted pure brass idol")
                .isActive(true)
                .build();

        when(productService.getProductBySlug("brass-kanha-idol")).thenReturn(response);

        mockMvc.perform(get("/products/brass-kanha-idol")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slug").value("brass-kanha-idol"));
    }
}
