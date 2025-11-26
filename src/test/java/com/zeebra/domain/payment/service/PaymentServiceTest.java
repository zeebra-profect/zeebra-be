package com.zeebra.domain.payment.service;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.zeebra.domain.order.entity.Order;
import com.zeebra.domain.order.entity.OrderItem;
import com.zeebra.domain.order.entity.OrderItemStatus;
import com.zeebra.domain.order.entity.OrderStatus;
import com.zeebra.domain.order.entity.OrderType;
import com.zeebra.domain.order.entity.ReturnStatus;
import com.zeebra.domain.order.repository.OrderHistoryRepository;
import com.zeebra.domain.order.repository.OrderItemRepository;
import com.zeebra.domain.order.repository.OrderRepository;
import com.zeebra.domain.payment.dto.CreatePaymentRequest;
import com.zeebra.domain.payment.dto.CreatePaymentResponse;
import com.zeebra.domain.payment.entity.Payment;
import com.zeebra.domain.payment.repository.PaymentHistoryRepository;
import com.zeebra.domain.payment.repository.PaymentRepository;
import com.zeebra.domain.payment.repository.PaymentTransactionRepository;
import com.zeebra.domain.product.entity.Product;
import com.zeebra.domain.product.entity.ProductOption;
import com.zeebra.domain.product.entity.Sales;
import com.zeebra.domain.product.entity.SalesStatus;
import com.zeebra.domain.product.repository.ProductOptionRepository;
import com.zeebra.domain.product.repository.ProductRepository;
import com.zeebra.domain.product.repository.SalesRepository;

@ActiveProfiles("test")
@SpringBootTest
public class PaymentServiceTest {

	@Autowired
	private PaymentRepository paymentRepository;
	@Autowired
	private PaymentHistoryRepository paymentHistoryRepository;
	@Autowired
	private PaymentTransactionRepository paymentTransactionRepository;
	@Autowired
	private ProductRepository productRepository;
	@Autowired
	private ProductOptionRepository productOptionRepository;
	@Autowired
	private OrderRepository orderRepository;
	@Autowired
	private OrderItemRepository orderItemRepository;
	@Autowired
	private OrderHistoryRepository orderHistoryRepository;
	@Autowired
	private SalesRepository salesRepository;

	@Autowired
	private PaymentService paymentService;

	@AfterEach
	void tearDown() {
		paymentRepository.deleteAllInBatch();
		paymentHistoryRepository.deleteAllInBatch();
		paymentTransactionRepository.deleteAllInBatch();
		productRepository.deleteAllInBatch();
		productOptionRepository.deleteAllInBatch();
		orderRepository.deleteAllInBatch();
		orderItemRepository.deleteAllInBatch();
		orderHistoryRepository.deleteAllInBatch();
		salesRepository.deleteAllInBatch();
	}

	private Product createProduct(){
		return productRepository.save(new Product(1L, 1L, "상품 이름", "테스트 상품", "Test_12A", "thumbnail_1.jpeg", List.of("product_1/1.jpeg", "product_1/2.jpeg")));
	}

	private ProductOption createProductOption(Long productId){
		return productOptionRepository.save(new ProductOption(productId));
	}

	private Sales createSales(Long productOptionId, BigDecimal price, int stock, SalesStatus salesStatus){
		return salesRepository.save(new Sales(productOptionId, 1L, price, stock, salesStatus));
	}

	private Order createOrder(String orderNumber, LocalDateTime orderTime, String clientRequestId){
		return orderRepository.save(Order.builder()
			.memberId(1L)
			.orderNumber(orderNumber)
			.orderStatus(OrderStatus.CREATED)
			.orderType(OrderType.DIRECT)
			.orderTime(orderTime)
			.totalQuantity(1)
			.totalPrice(BigDecimal.valueOf(10000))
			.totalAmount(BigDecimal.valueOf(10000))
			.tradeId(1L)
			.usePoint(0)
			.idempotencyKey(clientRequestId)
			.build());
	}

	private OrderItem createOrderItem(Long orderId, Long salesId){
		return orderItemRepository.save(OrderItem.builder()
			.orderId(orderId)
			.saleId(salesId)
			.orderItemName("테스트 주문 상품")
			.orderItemPrice(BigDecimal.valueOf(10000))
			.orderItemThumbnail("thumbnail_1.jpeg")
			.orderItemQuantity(1)
			.orderItemAmount(BigDecimal.valueOf(10000))
			.orderItemStatus(OrderItemStatus.CREATED)
			.refundableQuantity(1)
			.refundableAmount(BigDecimal.valueOf(10000))
			.returnedQuantity(0)
			.returnStatus(ReturnStatus.NONE)
			.returnReason(null)
			.build());
	}

	@DisplayName("주문 정보를 받아서 결제 정보를 생성합니다.")
	@Test
	void createPayment(){
		//given
		String orderName = "테스트 주문";
		String orderNumber = "20251114-1445238969";
		LocalDateTime orderTime = LocalDateTime.of(2025,11,14,14,45,30, 00);
		String clientRequestId = "idem-key-001";

		Product product = createProduct();
		ProductOption productOption = createProductOption(product.getId());
		Order order = createOrder(orderNumber, orderTime, clientRequestId);
		Sales sales = createSales(productOption.getId(), BigDecimal.valueOf(10000), 1, SalesStatus.ON_SALE);
		createOrderItem(order.getId(), sales.getId());
		CreatePaymentRequest request = new CreatePaymentRequest(order.getId(), orderName, BigDecimal.valueOf(10000), BigDecimal.ZERO, BigDecimal.valueOf(10000), clientRequestId );

		//when
		CreatePaymentResponse response = paymentService.createPayment(request, 1L);

		//then
		assertThat(response.paymentId()).isNotNull();
		assertThat(response.amount().value()).isEqualByComparingTo(BigDecimal.valueOf(10000));
		assertThat(response.tossOrderId()).startsWith("ORD_" + orderNumber);
		assertThat(response.orderName()).isEqualTo(orderName);

		Payment saved = paymentRepository.findById(response.paymentId()).orElseThrow();
		assertThat(saved.getPaymentAmount()).isEqualByComparingTo(BigDecimal.valueOf(10000));
		assertThat(saved.getIdempotencyKey()).isEqualTo(clientRequestId);
	}

	@DisplayName("동일 clientRequestId로 결제 정보 생성을 동시에 요청하면 UNIQUE 제약 조건에 의해 한 건만 생성됩니다.")
	@Test
	void createPaymentConcurrentSameClientRequestId() throws Exception {
		//given
		Long memberId = 1L;
		String clientRequestId = "idem-key-001";
		String orderName = "테스트 주문";
		String orderNumber = "20251114-1445238969";
		LocalDateTime orderTime = LocalDateTime.of(2025,11,14,14,45,30, 00);

		Product product = createProduct();
		ProductOption productOption = createProductOption(product.getId());
		Order order = createOrder(orderNumber, orderTime, clientRequestId);
		Sales sales = createSales(productOption.getId(), BigDecimal.valueOf(10000), 1, SalesStatus.ON_SALE);
		createOrderItem(order.getId(), sales.getId());
		CreatePaymentRequest request = new CreatePaymentRequest(order.getId(), orderName, BigDecimal.valueOf(10000), BigDecimal.ZERO, BigDecimal.valueOf(10000), clientRequestId );

		int threadCount = 2;
		ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch endLatch = new CountDownLatch(threadCount);

		AtomicReference<Throwable> throwable1 = new AtomicReference<>();
		AtomicReference<Throwable> throwable2 = new AtomicReference<>();
		AtomicReference<CreatePaymentResponse> response1 = new AtomicReference<>();
		AtomicReference<CreatePaymentResponse> response2 = new AtomicReference<>();

		executorService.submit(() -> {
			try {
				startLatch.await();
				response1.set(paymentService.createPayment(request, memberId));
			} catch (Throwable throwable) {
				throwable1.set(throwable);
			}  finally {
				endLatch.countDown();
			}
		});

		executorService.submit(() -> {
			try {
				startLatch.await();
				response2.set(paymentService.createPayment(request, memberId));
			} catch (Throwable throwable) {
				throwable2.set(throwable);
			} finally {
				endLatch.countDown();
			}
		});

		//when
		startLatch.countDown();
		endLatch.await();
		executorService.shutdown();

		//then
		List<Payment> payments = paymentRepository.findAll();
		Long createdCount = payments.stream()
			.filter(p -> clientRequestId.equals(p.getIdempotencyKey()))
			.count();

		assertThat(createdCount)
			.as("동일 clientRequestId에 대해 Payment는 최대 1건만 생성되어야 한다.")
			.isEqualTo(1L);

		int successCount = 0;
		int errorCount = 0;

		if (response1.get() != null) successCount++;
		if (response2.get() != null) successCount++;
		if (throwable1.get() != null) errorCount++;
		if (throwable2.get() != null) errorCount++;

		assertThat(successCount)
			.as("동시 요청 중 최소 하나는 결제 생성(또는 멱등성 재사용)에 성공해야 한다.")
			.isGreaterThanOrEqualTo(1);

		assertThat(errorCount)
			.as("UNIQUE 제약 및 멱등성 처리에 따라 실패는 최대 1개까지 허용된다.")
			.isLessThanOrEqualTo(1);
	}
}