package com.backend.kdt.pay.service;

import com.backend.kdt.auth.entity.User;
import com.backend.kdt.auth.repository.UserRepository;
import com.backend.kdt.pay.dto.ProductDetailDto;
import com.backend.kdt.pay.dto.ProductDto;
import com.backend.kdt.pay.entity.Product;
import com.backend.kdt.pay.entity.ProductExchange;
import com.backend.kdt.pay.entity.TransactionType;
import com.backend.kdt.pay.repository.ProductExchangeRepository;
import com.backend.kdt.pay.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    // 시청 완료 시 지급할 포인트 (상수)
    private static final Long WATCH_COMPLETION_POINT = 100L;

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductExchangeRepository exchangeRepository;

    /**
     * 상품 교환 - PURCHASE 타입으로 자동 처리
     */
    @Transactional
    @CacheEvict(value = {"product", "productsByCategory"}, allEntries = true)
    public void exchangeProduct(Long userId, Long productId, int quantity) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("유저 없음"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("상품 없음"));

        if (product.getTransactionType() == TransactionType.DONATION) {
            throw new IllegalArgumentException("기부 상품은 구매할 수 없습니다. 기부 API를 이용해주세요.");
        }

        int totalCost = product.getPointCost() * quantity;

        if (user.getPoint() < totalCost) {
            throw new IllegalArgumentException("포인트 부족");
        }

        if (product.getStock() < quantity) {
            throw new IllegalArgumentException("상품 재고 부족");
        }

        // 포인트 차감 & 재고 감소
        user.setPoint(user.getPoint() - totalCost);
        product.setStock(product.getStock() - quantity);

        // PURCHASE 타입으로 자동 설정
        ProductExchange exchange = ProductExchange.builder()
                .user(user)
                .product(product)
                .quantity(quantity)
                .totalCost(totalCost)
                .transactionType(TransactionType.PURCHASE)  // 자동으로 PURCHASE 설정
                .exchangedAt(LocalDateTime.now())
                .build();

        try {
            exchangeRepository.save(exchange);
        } catch (OptimisticLockingFailureException e) {
            throw new RuntimeException("상품 교환 중 충돌이 발생했습니다. 다시 시도해주세요.");
        }
    }

    /**
     * 기부하기 - DONATION 타입으로 자동 처리
     */
    @Transactional
    @CacheEvict(value = {"product", "productsByCategory"}, allEntries = true)
    public void donateProduct(Long userId, Long productId, int donationAmount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("유저 없음"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("상품 없음"));

        if (product.getTransactionType() != TransactionType.DONATION) {
            throw new IllegalArgumentException("일반 상품은 기부할 수 없습니다. 구매 API를 이용해주세요.");
        }

        if (user.getPoint() < donationAmount) {
            throw new IllegalArgumentException("포인트 부족");
        }

        // 포인트 차감
        user.setPoint(user.getPoint() - donationAmount);

        // DONATION 타입으로 자동 설정
        ProductExchange exchange = ProductExchange.builder()
                .user(user)
                .product(product)
                .quantity(1) // 기부는 수량 개념이 없으므로 1로 고정
                .totalCost(donationAmount)
                .transactionType(TransactionType.DONATION)  // 자동으로 DONATION 설정
                .exchangedAt(LocalDateTime.now())
                .accepted(true) // 기부는 바로 완료 처리
                .build();

        exchangeRepository.save(exchange);
    }

    /**
     * 카테고리별 상품 조회 (TransactionType별)
     */
    @Cacheable(value = "productsByCategory", key = "#transactionType")
    public List<ProductDto> getProductsByCategory(TransactionType transactionType) {
        return productRepository.findByTransactionType(transactionType).stream()
                .map(ProductDto::from)
                .toList();
    }

    /**
     * 개별 상품 조회
     */
    @Cacheable(value = "product", key = "#productId")
    public ProductDto getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("상품 없음"));
        return ProductDto.from(product);
    }

    /**
     * 사용자 상품 구매/기부 내역 조회
     */
    public List<ProductDetailDto> getUserProductHistory(Long userId) {
        return exchangeRepository.findByUserIdOrderByExchangedAtDesc(userId).stream()
                .map(ProductDetailDto::from)
                .toList();
    }

    /**
     * 교환 상세 정보 조회
     */
    @Cacheable(value = "productExchangeDetail", key = "#exchangeId")
    public ProductDetailDto getProductDetailByExchange(Long exchangeId) {
        ProductExchange exchange = exchangeRepository.findById(exchangeId)
                .orElseThrow(() -> new EntityNotFoundException("교환 내역 없음"));
        return ProductDetailDto.from(exchange);
    }

    /**
     * 상품 수락 상태 토글
     */
    @Transactional
    public void toggleProductAccepted(Long exchangeId) {
        ProductExchange exchange = exchangeRepository.findById(exchangeId)
                .orElseThrow(() -> new EntityNotFoundException("교환 기록 없음"));

        if (exchange.getTransactionType() == TransactionType.DONATION) {
            throw new IllegalArgumentException("기부 내역은 수락 상태를 변경할 수 없습니다.");
        }

        exchange.setAccepted(!exchange.isAccepted());
        exchangeRepository.save(exchange);
    }

    /**
     * 사용자 미수락 구매 내역 조회
     */
    public List<ProductDetailDto> getUserUnacceptedProductHistory(Long userId) {
        return exchangeRepository.findByUserIdAndAcceptedFalseAndTransactionTypeOrderByExchangedAtDesc(
                        userId, TransactionType.PURCHASE).stream()
                .map(ProductDetailDto::from)
                .toList();
    }

    /**
     * 사용자 기부 내역 조회
     */
    public List<ProductDetailDto> getUserDonationHistory(Long userId) {
        return exchangeRepository.findByUserIdAndTransactionTypeOrderByExchangedAtDesc(
                        userId, TransactionType.DONATION).stream()
                .map(ProductDetailDto::from)
                .toList();
    }

    /**
     * 시청 완료 처리 및 포인트 지급
     */
    @Transactional
    public void completeWatching(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("유저 없음"));

        // 이미 시청 완료한 경우 예외 처리
        if (user.getWatched()) {
            throw new IllegalStateException("이미 시청을 완료하였습니다.");
        }

        // 시청 완료 처리 및 포인트 지급
        user.setWatched(true);
        user.setPoint(user.getPoint() + WATCH_COMPLETION_POINT);

        userRepository.save(user);
    }

    /**
     * 시청 상태 조회
     */
    public boolean isWatchCompleted(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("유저 없음"));
        return user.getWatched();
    }

    /**
     * 관리자용 시청 상태 초기화
     */
    @Transactional
    public void resetWatchStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("유저 없음"));
        user.setWatched(false);
        userRepository.save(user);
    }
}