package com.backend.kdt.pay.controller;

import com.backend.kdt.auth.dto.ApiResponse;
import com.backend.kdt.pay.dto.DonationRequestDto;
import com.backend.kdt.pay.dto.ProductDetailDto;
import com.backend.kdt.pay.dto.ProductDto;
import com.backend.kdt.pay.dto.ProductExchangeRequestDto;
import com.backend.kdt.pay.entity.TransactionType;
import com.backend.kdt.pay.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "상품 API", description = "상품 조회 및 교환 관련 API")
public class ProductController {

    private final ProductService productService;

//    특산물 구매 + 갯수
@PostMapping("/{productId}/exchange")
@Operation(summary = "상품 교환", description = "사용자가 보유한 포인트로 상품을 교환합니다.")
public ResponseEntity<ApiResponse<String>> exchangeProduct(
        @Parameter(description = "상품 ID") @PathVariable Long productId,
        @RequestBody ProductExchangeRequestDto request) {
    productService.exchangeProduct(request.getUserId(), productId, request.getQuantity());
    return ResponseEntity.ok(ApiResponse.onSuccess("상품 교환 완료"));
}

    @GetMapping
    @Operation(summary = "상품 목록 조회", description = "카테고리에 따른 상품 리스트를 조회합니다.")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getProducts(
            @Parameter(description = "상품 카테고리") @RequestParam TransactionType transactionType) {
        List<ProductDto> products = productService.getProductsByCategory(transactionType);
        return ResponseEntity.ok(ApiResponse.onSuccess(products));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "(구매전) 단일 상품 조회", description = "상품 ID로 특정 상품 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<ProductDto>> getProduct(
            @Parameter(description = "상품 ID") @PathVariable Long productId) {
        ProductDto product = productService.getProduct(productId);
        return ResponseEntity.ok(ApiResponse.onSuccess(product));
    }

//    @Operation(summary = "구매한 상품 보관함 조회", description = "사용자가 교환한 상품 목록을 전체 조회합니다.")
//    @GetMapping("/my")
//    public ResponseEntity<ApiResponse<List<ProductDetailDto>>> getMyProducts(
//            @Parameter(description = "사용자 ID") @RequestParam Long userId) {
//        List<ProductDetailDto> products = productService.getUserProductHistory(userId);
//        return ResponseEntity.ok(ApiResponse.onSuccess(products));
//    }
//
//    @GetMapping("/exchanges/{exchangeId}/detail")
//    @Operation(summary = "교환 완료 상품 상세 조회", description = "상품 교환 내역 ID로 상세 정보를 조회합니다.")
//    public ResponseEntity<ApiResponse<ProductDetailDto>> getProductDetailByExchange(
//            @Parameter(description = "교환 ID") @PathVariable Long exchangeId) {
//        ProductDetailDto detail = productService.getProductDetailByExchange(exchangeId);
//        return ResponseEntity.ok(ApiResponse.onSuccess(detail));
//    }


//    기부하기
@PostMapping("/{productId}/donate")
@Operation(summary = "기부하기", description = "사용자가 보유한 포인트로 기부합니다.")
public ResponseEntity<ApiResponse<String>> donateProduct(
        @Parameter(description = "상품 ID") @PathVariable Long productId,
        @RequestBody DonationRequestDto request) {
    productService.donateProduct(request.getUserId(), productId, request.getDonationAmount());
    return ResponseEntity.ok(ApiResponse.onSuccess("기부 완료"));
}

    @Operation(summary = "기부 내역 조회", description = "사용자의 기부 내역을 조회합니다.")
    @GetMapping("/my/donations")
    public ResponseEntity<ApiResponse<List<ProductDetailDto>>> getMyDonations(
            @Parameter(description = "사용자 ID") @RequestParam Long userId) {
        List<ProductDetailDto> donations = productService.getUserDonationHistory(userId);
        return ResponseEntity.ok(ApiResponse.onSuccess(donations));
    }

//    영상 시청
    @Operation(summary = "시청 완료 처리", description = "영상 시청을 완료하고 포인트를 지급합니다. (1회만 가능)")
    @PostMapping("/complete-watching")
    public ResponseEntity<ApiResponse<String>> completeWatching(
            @Parameter(description = "유저 ID") @RequestParam Long userId) {
        try {
            productService.completeWatching(userId);
            return ResponseEntity.ok(ApiResponse.onSuccess("시청 완료! 포인트가 지급되었습니다."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.onFailure("ALREADY_WATCHED", e.getMessage()));
        }
    }

    @Operation(summary = "시청 상태 조회", description = "유저의 시청 완료 여부를 조회합니다.")
    @GetMapping("/watch-status")
    public ResponseEntity<ApiResponse<Boolean>> getWatchStatus(
            @Parameter(description = "유저 ID") @RequestParam Long userId) {
        boolean isCompleted = productService.isWatchCompleted(userId);
        return ResponseEntity.ok(ApiResponse.onSuccess(isCompleted));
    }
}