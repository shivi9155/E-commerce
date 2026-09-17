package com.kanhacollection.backend.coupon;

import com.kanhacollection.backend.coupon.dto.CouponResponse;
import com.kanhacollection.backend.coupon.dto.CouponValidationResponse;
import com.kanhacollection.backend.coupon.dto.CreateCouponRequest;
import com.kanhacollection.backend.coupon.dto.ValidateCouponRequest;
import com.kanhacollection.backend.exception.DuplicateResourceException;
import com.kanhacollection.backend.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponServiceImpl Unit Tests")
class CouponServiceImplTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponServiceImpl couponService;

    private Coupon buildActiveCoupon(DiscountType type, BigDecimal value, BigDecimal minOrder, BigDecimal maxDiscount) {
        return Coupon.builder()
                .id(UUID.randomUUID())
                .code("DIWALI20")
                .discountType(type)
                .discountValue(value)
                .minOrderValue(minOrder != null ? minOrder : BigDecimal.ZERO)
                .maxDiscountAmount(maxDiscount)
                .usageLimit(100)
                .perUserLimit(1)
                .currentUsage(0)
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("validateCoupon()")
    class ValidateCouponTests {

        @Test
        @DisplayName("Should apply percentage discount correctly")
        void shouldApplyPercentageDiscount() {
            Coupon coupon = buildActiveCoupon(DiscountType.PERCENTAGE, BigDecimal.valueOf(20), BigDecimal.ZERO, BigDecimal.valueOf(500));
            when(couponRepository.findByCode("DIWALI20")).thenReturn(Optional.of(coupon));

            ValidateCouponRequest request = new ValidateCouponRequest();
            request.setCode("diwali20");
            request.setCartSubtotal(BigDecimal.valueOf(1500));

            CouponValidationResponse result = couponService.validateCoupon(UUID.randomUUID(), request);

            assertThat(result.isValid()).isTrue();
            assertThat(result.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(300)); // 20% of 1500 = 300
            assertThat(result.getFinalSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(1200));
            assertThat(result.getMessage()).contains("successfully");
        }

        @Test
        @DisplayName("Should cap percentage discount at maxDiscountAmount")
        void shouldCapPercentageDiscountAtMax() {
            Coupon coupon = buildActiveCoupon(DiscountType.PERCENTAGE, BigDecimal.valueOf(20), BigDecimal.ZERO, BigDecimal.valueOf(200));
            when(couponRepository.findByCode("DIWALI20")).thenReturn(Optional.of(coupon));

            ValidateCouponRequest request = new ValidateCouponRequest();
            request.setCode("diwali20");
            request.setCartSubtotal(BigDecimal.valueOf(5000));

            CouponValidationResponse result = couponService.validateCoupon(UUID.randomUUID(), request);

            assertThat(result.isValid()).isTrue();
            // 20% of 5000 = 1000, but capped at 200
            assertThat(result.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
            assertThat(result.getFinalSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(4800));
        }

        @Test
        @DisplayName("Should apply fixed discount correctly")
        void shouldApplyFixedDiscount() {
            Coupon coupon = buildActiveCoupon(DiscountType.FIXED, BigDecimal.valueOf(100), BigDecimal.ZERO, null);
            when(couponRepository.findByCode("DIWALI20")).thenReturn(Optional.of(coupon));

            ValidateCouponRequest request = new ValidateCouponRequest();
            request.setCode("diwali20");
            request.setCartSubtotal(BigDecimal.valueOf(500));

            CouponValidationResponse result = couponService.validateCoupon(UUID.randomUUID(), request);

            assertThat(result.isValid()).isTrue();
            assertThat(result.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
            assertThat(result.getFinalSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(400));
        }

        @Test
        @DisplayName("Should cap fixed discount at cart subtotal (discount cannot exceed cart value)")
        void shouldCapFixedDiscountAtSubtotal() {
            Coupon coupon = buildActiveCoupon(DiscountType.FIXED, BigDecimal.valueOf(500), BigDecimal.ZERO, null);
            when(couponRepository.findByCode("DIWALI20")).thenReturn(Optional.of(coupon));

            ValidateCouponRequest request = new ValidateCouponRequest();
            request.setCode("diwali20");
            request.setCartSubtotal(BigDecimal.valueOf(200));

            CouponValidationResponse result = couponService.validateCoupon(UUID.randomUUID(), request);

            assertThat(result.isValid()).isTrue();
            // discount capped at 200 (the cart value)
            assertThat(result.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
            assertThat(result.getFinalSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should reject invalid coupon code")
        void shouldRejectInvalidCouponCode() {
            when(couponRepository.findByCode("INVALIDCODE")).thenReturn(Optional.empty());

            ValidateCouponRequest request = new ValidateCouponRequest();
            request.setCode("invalidcode");
            request.setCartSubtotal(BigDecimal.valueOf(1000));

            CouponValidationResponse result = couponService.validateCoupon(UUID.randomUUID(), request);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessage()).containsIgnoringCase("invalid");
        }

        @Test
        @DisplayName("Should reject expired coupon")
        void shouldRejectExpiredCoupon() {
            Coupon coupon = buildActiveCoupon(DiscountType.PERCENTAGE, BigDecimal.valueOf(10), BigDecimal.ZERO, null);
            coupon.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS)); // expired yesterday
            when(couponRepository.findByCode("DIWALI20")).thenReturn(Optional.of(coupon));

            ValidateCouponRequest request = new ValidateCouponRequest();
            request.setCode("diwali20");
            request.setCartSubtotal(BigDecimal.valueOf(1000));

            CouponValidationResponse result = couponService.validateCoupon(UUID.randomUUID(), request);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessage()).containsIgnoringCase("expired");
        }

        @Test
        @DisplayName("Should reject coupon when cart subtotal is below minimum order value")
        void shouldRejectWhenBelowMinOrderValue() {
            Coupon coupon = buildActiveCoupon(DiscountType.PERCENTAGE, BigDecimal.valueOf(15), BigDecimal.valueOf(999), null);
            when(couponRepository.findByCode("DIWALI20")).thenReturn(Optional.of(coupon));

            ValidateCouponRequest request = new ValidateCouponRequest();
            request.setCode("diwali20");
            request.setCartSubtotal(BigDecimal.valueOf(500));

            CouponValidationResponse result = couponService.validateCoupon(UUID.randomUUID(), request);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessage()).contains("Minimum");
        }

        @Test
        @DisplayName("Should reject inactive coupon")
        void shouldRejectInactiveCoupon() {
            Coupon coupon = buildActiveCoupon(DiscountType.FIXED, BigDecimal.valueOf(50), BigDecimal.ZERO, null);
            coupon.setActive(false);
            when(couponRepository.findByCode("DIWALI20")).thenReturn(Optional.of(coupon));

            ValidateCouponRequest request = new ValidateCouponRequest();
            request.setCode("diwali20");
            request.setCartSubtotal(BigDecimal.valueOf(1000));

            CouponValidationResponse result = couponService.validateCoupon(UUID.randomUUID(), request);

            assertThat(result.isValid()).isFalse();
        }
    }

    @Nested
    @DisplayName("createCoupon()")
    class CreateCouponTests {

        @Test
        @DisplayName("Should create coupon successfully with valid input")
        void shouldCreateCouponSuccessfully() {
            CreateCouponRequest request = new CreateCouponRequest();
            request.setCode("navratri30");
            request.setDiscountType(DiscountType.PERCENTAGE);
            request.setDiscountValue(BigDecimal.valueOf(30));
            request.setMinOrderValue(BigDecimal.valueOf(500));
            request.setMaxDiscountAmount(BigDecimal.valueOf(300));
            request.setUsageLimit(50);
            request.setPerUserLimit(2);
            request.setExpiresAt(Instant.now().plus(60, ChronoUnit.DAYS));

            when(couponRepository.existsByCode("NAVRATRI30")).thenReturn(false);
            when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> {
                Coupon saved = inv.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            CouponResponse response = couponService.createCoupon(request);

            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo("NAVRATRI30");
            assertThat(response.getDiscountType()).isEqualTo(DiscountType.PERCENTAGE);
            verify(couponRepository, times(1)).save(any(Coupon.class));
        }

        @Test
        @DisplayName("Should throw DuplicateResourceException when coupon code already exists")
        void shouldThrowException_WhenDuplicateCode() {
            CreateCouponRequest request = new CreateCouponRequest();
            request.setCode("DIWALI20");
            request.setDiscountType(DiscountType.FIXED);
            request.setDiscountValue(BigDecimal.valueOf(100));

            when(couponRepository.existsByCode("DIWALI20")).thenReturn(true);

            assertThatThrownBy(() -> couponService.createCoupon(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("DIWALI20");
        }
    }

    @Nested
    @DisplayName("deleteCoupon()")
    class DeleteCouponTests {

        @Test
        @DisplayName("Should soft-delete coupon by setting isActive to false")
        void shouldSoftDeleteCoupon() {
            UUID couponId = UUID.randomUUID();
            Coupon coupon = buildActiveCoupon(DiscountType.FIXED, BigDecimal.valueOf(50), BigDecimal.ZERO, null);
            coupon.setId(couponId);
            when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
            when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

            couponService.deleteCoupon(couponId);

            assertThat(coupon.isActive()).isFalse();
            verify(couponRepository, times(1)).save(coupon);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when coupon not found")
        void shouldThrowException_WhenCouponNotFound() {
            UUID couponId = UUID.randomUUID();
            when(couponRepository.findById(couponId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> couponService.deleteCoupon(couponId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
