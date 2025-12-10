// package com.zeebra.domain.order.service;
//
// import static org.assertj.core.api.Assertions.*;
// import static org.mockito.BDDMockito.*;
//
// import java.math.BigDecimal;
// import java.time.LocalDate;
// import java.util.List;
// import java.util.stream.IntStream;
//
// import org.junit.jupiter.api.AfterEach;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.data.domain.PageRequest;
// import org.springframework.jdbc.core.JdbcTemplate;
// import org.springframework.test.context.ActiveProfiles;
// import org.springframework.test.context.bean.override.mockito.MockitoBean;
//
// import com.zeebra.domain.cart.entity.Cart;
// import com.zeebra.domain.cart.entity.CartItem;
// import com.zeebra.domain.cart.repository.CartItemRepository;
// import com.zeebra.domain.cart.repository.CartRepository;
// import com.zeebra.domain.chat.entity.ChatRoom;
// import com.zeebra.domain.chat.entity.ChatRoomType;
// import com.zeebra.domain.chat.entity.Trade;
// import com.zeebra.domain.order.dto.CreateOrderRequest;
// import com.zeebra.domain.order.dto.CreateOrderResponse;
// import com.zeebra.domain.order.dto.OrderInfo;
// import com.zeebra.domain.order.dto.OrderItemResponse;
// import com.zeebra.domain.order.dto.OrderResponse;
// import com.zeebra.domain.order.dto.ProductInfo;
// import com.zeebra.domain.order.dto.ReadOrderListResponse;
// import com.zeebra.domain.order.dto.SalesItem;
// import com.zeebra.domain.order.entity.Order;
// import com.zeebra.domain.order.entity.OrderItem;
// import com.zeebra.domain.order.entity.OrderItemStatus;
// import com.zeebra.domain.order.entity.OrderStatus;
// import com.zeebra.domain.order.repository.OrderHistoryRepository;
// import com.zeebra.domain.order.repository.OrderItemRepository;
// import com.zeebra.domain.order.repository.OrderRepository;
// import com.zeebra.domain.product.entity.Product;
// import com.zeebra.domain.product.entity.ProductOption;
// import com.zeebra.domain.product.entity.Sales;
// import com.zeebra.domain.product.entity.SalesStatus;
// import com.zeebra.domain.product.repository.ProductOptionRepository;
// import com.zeebra.domain.product.repository.ProductRepository;
// import com.zeebra.domain.product.repository.SalesRepository;
// import com.zeebra.domain.product.service.ProductInfoService;
// import com.zeebra.global.ErrorCode.CommonErrorCode;
// import com.zeebra.global.exception.BusinessException;
//
// @ActiveProfiles("test")
// @SpringBootTest
// public class OrderServiceTest {
//
// 	@Autowired
// 	JdbcTemplate jdbcTemplate;
// 	@MockitoBean
// 	private ProductInfoService productInfoService;
// 	@Autowired
// 	private ProductRepository productRepository;
// 	@Autowired
// 	private ProductOptionRepository productOptionRepository;
// 	@Autowired
// 	private SalesRepository salesRepository;
// 	@Autowired
// 	private CartRepository cartRepository;
// 	@Autowired
// 	private CartItemRepository cartItemRepository;
// 	@Autowired
// 	private OrderRepository orderRepository;
// 	@Autowired
// 	private OrderItemRepository orderItemRepository;
// 	@Autowired
// 	private OrderHistoryRepository orderHistoryRepository;
// 	@Autowired
// 	private OrderService orderService;
//
// 	@AfterEach
// 	void tearDown() {
// 		orderItemRepository.deleteAllInBatch();
// 		orderHistoryRepository.deleteAllInBatch();
// 		orderRepository.deleteAllInBatch();
// 		productOptionRepository.deleteAllInBatch();
// 		productRepository.deleteAllInBatch();
// 		salesRepository.deleteAllInBatch();
// 		cartItemRepository.deleteAllInBatch();
// 		cartRepository.deleteAllInBatch();
// 	}
//
//
// 	@BeforeEach
// 	void setUp() {
// 		jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS order_number_seq START WITH 1 INCREMENT BY 1");
// 	}
// 	private Product createProduct(){
// 		return productRepository.save(new Product(1L, 1L, "상품 이름", "테스트 상품", "Test_12A", "thumbnail_1.jpeg", List.of("product_1/1.jpeg", "product_1/2.jpeg")));
// 	}
//
// 	private ProductOption createProductOption(Long productId){
// 		return productOptionRepository.save(new ProductOption(productId));
// 	}
//
// 	private Sales createSales(Long productOptionId, BigDecimal price, int stock, SalesStatus salesStatus){
// 		return salesRepository.save(new Sales(productOptionId, 1L, price, stock, salesStatus));
// 	}
//
// 	private Cart createCart(Long memberId){
// 		return cartRepository.save(new Cart(memberId));
// 	}
//
// 	private CartItem createCartItem(Long cartId, Long productOptionId, BigDecimal price, Integer quantity){
// 		return cartItemRepository.save(new CartItem(cartId, productOptionId, price, quantity));
// 	}
//
// 	private void mockProductInfoService(Sales sales, ProductOption productOption, Product product) {
// 		ProductInfo mockProductInfo = ProductInfo.of(
// 			sales.getId(),
// 			productOption.getId(),
// 			product.getName(),
// 			product.getThumbnail(),
// 			List.of()
// 		);
// 		given(productInfoService.getProductInfoBySalesId(anyLong())).willReturn(mockProductInfo);
// 	}
//
// 	@DisplayName("장바구니 번호를 받아 장바구니 주문을 생성합니다.")
// 	@Test
// 	void createOrderByCartId(){
// 		//given
// 		Product product = createProduct();
// 		ProductOption productOption = createProductOption(product.getId());
// 		Sales sales = createSales(productOption.getId(), BigDecimal.valueOf(50000),1,  SalesStatus.ON_SALE);
// 		createSales(productOption.getId(), BigDecimal.valueOf(60000), 1, SalesStatus.ON_SALE);
// 		Cart cart = createCart(1L);
// 		createCartItem(cart.getId(), productOption.getId(), BigDecimal.valueOf(60000), 1);
// 		CreateOrderRequest request = CreateOrderRequest.fromCart("idem-key-001", cart.getId());
//
// 		mockProductInfoService(sales, productOption, product);
//
// 		//when
// 		CreateOrderResponse response = orderService.createOrder(1L, request);
//
// 		//then
// 		assertThat(response.order().orderId()).isNotNull();
// 		assertThat(response.order().orderStatus()).isEqualTo(OrderStatus.CREATED);
// 		assertThat(response.order().totalQuantity()).isEqualTo(1);
// 		assertThat(response.order().totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(50000));
// 		assertThat(response.order().totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(50000));
// 		OrderItemResponse itemResponse = response.order().orderItems().get(0);
// 		assertThat(itemResponse.orderItemId()).isNotNull();
// 		assertThat(itemResponse.saleId()).isEqualTo(sales.getId());
// 		assertThat(itemResponse.orderItemName()).isEqualTo(product.getName());
// 		assertThat(itemResponse.orderItemPrice()).isEqualByComparingTo(BigDecimal.valueOf(50000));
// 		assertThat(itemResponse.orderItemQuantity()).isEqualTo(1);
// 		assertThat(itemResponse.orderItemAmount()).isEqualByComparingTo(BigDecimal.valueOf(50000));
// 		assertThat(itemResponse.orderItemStatus()).isEqualTo(OrderItemStatus.CREATED);
// 	}
//
// 	@DisplayName("상품 옵션 번호를 받아 즉시구매 주문을 생성합니다.")
// 	@Test
// 	void createOrderByProductOptionId(){
// 		//given
// 		Product product = createProduct();
// 		ProductOption productOption = createProductOption(product.getId());
// 		Sales sales = createSales(productOption.getId(), BigDecimal.valueOf(60000), 1, SalesStatus.ON_SALE);
// 		CreateOrderRequest request = CreateOrderRequest.fromDirect("idem-key-001", productOption.getId());
// 		mockProductInfoService(sales, productOption, product);
// 		//when
// 		CreateOrderResponse response = orderService.createOrder(1L, request);
//
// 		//then
// 		assertThat(response.order().orderId()).isNotNull();
// 		assertThat(response.order().orderStatus()).isEqualTo(OrderStatus.CREATED);
// 		assertThat(response.order().totalQuantity()).isEqualTo(1);
// 		assertThat(response.order().totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(60000));
// 		assertThat(response.order().totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(60000));
// 		OrderItemResponse itemResponse = response.order().orderItems().get(0);
// 		assertThat(itemResponse.orderItemId()).isNotNull();
// 		assertThat(itemResponse.saleId()).isEqualTo(sales.getId());
// 		assertThat(itemResponse.orderItemName()).isEqualTo(product.getName());
// 		assertThat(itemResponse.orderItemPrice()).isEqualByComparingTo(BigDecimal.valueOf(60000));
// 		assertThat(itemResponse.orderItemQuantity()).isEqualTo(1);
// 		assertThat(itemResponse.orderItemAmount()).isEqualByComparingTo(BigDecimal.valueOf(60000));
// 		assertThat(itemResponse.orderItemStatus()).isEqualTo(OrderItemStatus.CREATED);
// 	}
//
// 	@DisplayName("거래 정보를 받아 채팅 거래 주문을 생성합니다.")
// 	@Test
// 	void createOrderBySalesItem(){
// 		//given
// 		Product product = createProduct();
// 		ProductOption productOption = createProductOption(product.getId());
// 		Sales sales = createSales(productOption.getId(), BigDecimal.valueOf(60000), 1, SalesStatus.ON_SALE);
// 		Trade trade = Trade.builder().chatRoom(ChatRoom.builder().saleId(sales.getId()).chatRoomType(ChatRoomType.DM).build()).price(BigDecimal.valueOf(50000)).build();
// 		SalesItem salesItem = SalesItem.of(trade.getId(), sales.getId(), 1, BigDecimal.valueOf(50000));
// 		CreateOrderRequest request = CreateOrderRequest.fromSalesItem("idem-key-001", salesItem);
// 		mockProductInfoService(sales, productOption, product);
// 		//when
// 		CreateOrderResponse response = orderService.createOrder(1L, request);
//
// 		//then
// 		assertThat(response.order().orderId()).isNotNull();
// 		assertThat(response.order().orderStatus()).isEqualTo(OrderStatus.CREATED);
// 		assertThat(response.order().totalQuantity()).isEqualTo(1);
// 		assertThat(response.order().totalPrice()).isEqualTo(BigDecimal.valueOf(50000));
// 		assertThat(response.order().totalAmount()).isEqualTo(BigDecimal.valueOf(50000));
// 		OrderItemResponse itemResponse = response.order().orderItems().get(0);
// 		assertThat(itemResponse.orderItemId()).isNotNull();
// 		assertThat(itemResponse.saleId()).isEqualTo(sales.getId());
// 		assertThat(itemResponse.orderItemName()).isEqualTo(product.getName());
// 		assertThat(itemResponse.orderItemPrice()).isEqualTo(BigDecimal.valueOf(50000));
// 		assertThat(itemResponse.orderItemQuantity()).isEqualTo(1);
// 		assertThat(itemResponse.orderItemAmount()).isEqualTo(BigDecimal.valueOf(50000));
// 		assertThat(itemResponse.orderItemStatus()).isEqualTo(OrderItemStatus.CREATED);
// 	}
//
// 	@DisplayName("같은 clientRequestId로 두 번 요청하면 두 번째는 새 주문을 생성하지 않고 첫 주문을 그대로 반환합니다.")
// 	@Test
// 	void createOrderWithSameClientRequestId(){
// 		//given
// 		Product product = createProduct();
// 		ProductOption productOption = createProductOption(product.getId());
// 		Sales sales = createSales(productOption.getId(), BigDecimal.valueOf(50000), 1, SalesStatus.ON_SALE);
// 		Cart cart = createCart(1L);
// 		createCartItem(cart.getId(), productOption.getId(), BigDecimal.valueOf(60000), 1);
// 		CreateOrderRequest request = CreateOrderRequest.fromCart("idem-key-001", cart.getId());
// 		mockProductInfoService(sales, productOption, product);
// 		//when
// 		CreateOrderResponse first = orderService.createOrder(1L, request);
// 		Long orderCountAfterFirst = orderRepository.count();
// 		CreateOrderResponse second = orderService.createOrder(1L, request);
// 		Long orderCountAfterSecond = orderRepository.count();
//
// 		//then
// 		assertThat(first.order().orderId()).isEqualTo(second.order().orderId());
// 		assertThat(orderCountAfterFirst).isEqualTo(orderCountAfterSecond);
// 		assertThat(first.order().orderStatus()).isEqualTo(second.order().orderStatus());
// 	}
//
// 	@DisplayName("장바구니 주문인데, 장바구니가 생성되어 있지 않으면 예외가 발생합니다.")
// 	@Test
// 	void createOrderByCartIdWithNonExistentCart(){
// 		//given
// 		CreateOrderRequest request = CreateOrderRequest.fromCart("idem-key-001", 1L);
// 		//when //then
// 		assertThatThrownBy(() -> orderService.createOrder(1L, request)).isInstanceOf(BusinessException.class).hasMessage("해당 장바구니를 찾을 수 없습니다.");
// 	}
//
// 	@DisplayName("장바구니가 비어있으면 주문 생성에 실패합니다.")
// 	@Test
// 	void createOrderWithEmptyCart(){
// 		//given
// 		Cart emptyCart = createCart(1L);
// 		CreateOrderRequest request = CreateOrderRequest.fromCart("idem-key-001", emptyCart.getId());
//
// 		//when //then
// 		assertThatThrownBy(() -> orderService.createOrder(1L, request) ).isInstanceOf(BusinessException.class).hasMessage("유효하지 않은 주문 요청입니다");
// 	}
//
// 	@DisplayName("장바구니 주문 시 해당 상품에 재고가 없으면 주문 생성에 실패합니다.")
// 	@Test
// 	void createOrderByCartIdWithOutOfStockProductOption(){
// 		//given
// 		Product product = createProduct();
// 		ProductOption productOption = createProductOption(product.getId());
// 		Cart cart = createCart(1L);
// 		createCartItem(cart.getId(), productOption.getId(), BigDecimal.valueOf(60000), 1);
// 		CreateOrderRequest request = CreateOrderRequest.fromCart("idem-key-001", cart.getId());
// 		//when //then
// 		assertThatThrownBy(() -> orderService.createOrder(1L, request)).isInstanceOf(BusinessException.class).hasMessage("상품 재고가 부족합니다.");
// 	}
//
// 	@DisplayName("장바구니 주문 시 해당 상품에 판매 중인 sales가 없으면 주문 생성에 실패합니다.")
// 	@Test
// 	void createOrderByCartIdWithNonExistentSalesId(){
// 		//given
// 		Product product = createProduct();
// 		ProductOption productOption = createProductOption(product.getId());
// 		Sales sales = createSales(productOption.getId(), BigDecimal.valueOf(50000), 1, SalesStatus.CONFIRMED);
// 		Cart cart = createCart(1L);
// 		createCartItem(cart.getId(), productOption.getId(), BigDecimal.valueOf(60000), 1);
// 		CreateOrderRequest request = CreateOrderRequest.fromCart("idem-key-001", cart.getId());
//
// 		//when //then
// 		assertThatThrownBy(()-> orderService.createOrder(1L, request)).isInstanceOf(BusinessException.class).hasMessage("상품 재고가 부족합니다.");
// 	}
//
// 	@DisplayName("장바구니 주문 시 같은 옵션이 여러 개 담겼고, 요구 수량보다 세일즈가 적으면 예외가 발생햡니다.")
// 	@Test
// 	void createOrderByCartIdWithMoreThanSalesStock(){
// 		//given
// 		Product product = createProduct();
// 		ProductOption productOption = createProductOption(product.getId());
// 		Sales sales = createSales(productOption.getId(), BigDecimal.valueOf(50000), 1, SalesStatus.ON_SALE);
// 		Cart cart = createCart(1L);
// 		createCartItem(cart.getId(), productOption.getId(), BigDecimal.valueOf(60000), 2);
// 		CreateOrderRequest request = CreateOrderRequest.fromCart("idem-key-001", cart.getId());
//
// 		//when //then
// 		assertThatThrownBy(() -> orderService.createOrder(1L, request)).isInstanceOf(BusinessException.class).hasMessage("상품 재고가 부족합니다.");
// 	}
//
// 	@DisplayName("즉시 구매 주문 시 해당 상품이 없는 상품이면 예외가 발생합니다.")
// 	@Test
// 	void createOrderByProductOptionIdWithNonExistentProductOptionId(){
// 		//given
// 		Long nonExistentProductOptionId = 9999L;
// 		CreateOrderRequest request = CreateOrderRequest.fromDirect("idem-key-001", nonExistentProductOptionId);
//
// 		//when //then
// 		assertThatThrownBy(()-> orderService.createOrder(1L, request)).isInstanceOf(BusinessException.class).hasMessage(
// 			"존재하지 않는 상품입니다.");
// 	}
//
// 	@DisplayName("즉시 구매 주문 시 해당 상품에 재고가 없으면 주문 생성에 실패합니다.")
// 	@Test
// 	void createOrderByProductOptionIdWithOutOfStockProductOption(){
// 		//given
// 		Product product = createProduct();
// 		ProductOption productOption = createProductOption(product.getId());
//
// 		CreateOrderRequest request = CreateOrderRequest.fromDirect("idem-key-001", productOption.getId());
//
// 		//when //then
// 		assertThatThrownBy(()-> orderService.createOrder(1L, request)).isInstanceOf(BusinessException.class).hasMessage("판매 중인 상품을 찾을 수 없습니다.");
// 	}
//
// 	@DisplayName("즉시 구매 주문 시 해당 상품에 판매 중인 sales가 없으면 주문 생성에 실패합니다.")
// 	@Test
// 	void createOrderByProductOptionIdWithNonExistentSalesId(){
// 		//given
// 		Product product = createProduct();
// 		ProductOption productOption = createProductOption(product.getId());
// 		Sales sales = createSales(productOption.getId(), BigDecimal.valueOf(50000), 1, SalesStatus.CONFIRMED);
//
// 		CreateOrderRequest request = CreateOrderRequest.fromDirect("idem-key-001", productOption.getId());
//
// 		//when //then
// 		assertThatThrownBy(()-> orderService.createOrder(1L, request)).isInstanceOf(BusinessException.class).hasMessage("판매 중인 상품을 찾을 수 없습니다.");
// 	}
//
// 	@DisplayName("채팅 거래 주문 시 SalesItem의 salesId가 판매 중이 아니면 주문 생성에 실패합니다.")
// 	@Test
// 	void createOrderBySalesItemWithNonExistentSalesId(){
// 		//given
// 		Product product = createProduct();
// 		ProductOption productOption = createProductOption(product.getId());
// 		Long invalidSalesId = 9999L;
// 		Trade trade = Trade.builder().chatRoom(ChatRoom.builder().saleId(invalidSalesId).chatRoomType(ChatRoomType.DM).build()).price(BigDecimal.valueOf(50000)).build();
// 		SalesItem salesItem = SalesItem.of(trade.getId(), invalidSalesId, 1, BigDecimal.valueOf(50000));
// 		CreateOrderRequest request = CreateOrderRequest.fromSalesItem("idem-key-001", salesItem);
//
// 		//when //then
// 		assertThatThrownBy(() -> orderService.createOrder(1L, request)).isInstanceOf(BusinessException.class).hasMessage("해당 판매 상품을 찾을 수 없습니다.");
// 	}
//
// 	@DisplayName("주문 Id와 회원 Id로 단일 주문 정보를 조회합니다.")
// 	@Test
// 	void getOrder(){
// 		//given
// 		Long memberId = 1L;
// 		Product product = createProduct();
// 		ProductOption productOption = createProductOption(product.getId());
//
// 		Sales sales = createSales(productOption.getId(), BigDecimal.valueOf(50000), 1, SalesStatus.ON_SALE);
//
// 		Cart cart = createCart(memberId);
// 		createCartItem(cart.getId(), productOption.getId(), BigDecimal.valueOf(60000), 1);
//
// 		String clientRequestId = "idem-key-001";
// 		CreateOrderRequest request = CreateOrderRequest.fromCart(clientRequestId, cart.getId());
//
// 		mockProductInfoService(sales, productOption, product);
//
// 		CreateOrderResponse createOrderResponse = orderService.createOrder(memberId, request);
// 		Long orderId = createOrderResponse.order().orderId();
//
// 		//when
// 		OrderInfo orderInfo = orderService.getOrder(memberId, orderId);
//
// 		//then
// 		assertThat(orderInfo.orderId()).isEqualTo(orderId);
// 		assertThat(orderInfo.orderNumber()).isEqualTo(createOrderResponse.order().orderNumber());
// 		assertThat(orderInfo.orderStatus()).isEqualTo(OrderStatus.CREATED);
// 		assertThat(orderInfo.orderTime()).isEqualToIgnoringNanos(createOrderResponse.order().orderTime());
// 		assertThat(orderInfo.totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(50000));
// 		assertThat(orderInfo.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(50000));
// 		assertThat(orderInfo.usePoint()).isEqualTo(0);
// 	}
//
// 	@DisplayName("본인 주문이 아니면 주문 정보 조회 시 예외가 발생합니다.")
// 	@Test
// 	void getOrderWithNonExistentMemberId(){
// 		//given
// 		Product product = createProduct();
//
// 		ProductOption productOption = createProductOption(product.getId());
//
// 		Sales sales = createSales(productOption.getId(), BigDecimal.valueOf(50000), 1, SalesStatus.ON_SALE);
//
// 		Cart cart = createCart(1L);
// 		createCartItem(cart.getId(), productOption.getId(), BigDecimal.valueOf(60000), 1);
//
// 		String clientRequestId = "idem-key-001";
// 		CreateOrderRequest request = CreateOrderRequest.fromCart(clientRequestId, cart.getId());
//
// 		mockProductInfoService(sales, productOption, product);
//
// 		CreateOrderResponse createOrderResponse = orderService.createOrder(1L, request);
// 		Long orderId = createOrderResponse.order().orderId();
//
// 		//when //then
// 		assertThatThrownBy(() -> orderService.getOrder(2L, orderId)).isInstanceOf(BusinessException.class).hasMessage("주문을 찾을 수 없습니다");
// 	}
//
// 	@DisplayName("주문 상세 조회 시 주문 정보와 주문 상품 목록을 함께 조회합니다.")
// 	@Test
// 	void getOrderDetail(){
// 		//given
// 		Product product = createProduct();
//
// 		ProductOption productOption = createProductOption(product.getId());
//
// 		Sales sales = createSales(productOption.getId(), BigDecimal.valueOf(50000), 1, SalesStatus.ON_SALE);
//
// 		Cart cart = createCart(1L);
// 		createCartItem(cart.getId(), productOption.getId(), BigDecimal.valueOf(60000), 1);
//
// 		String clientRequestId = "idem-key-001";
// 		CreateOrderRequest request = CreateOrderRequest.fromCart(clientRequestId, cart.getId());
//
// 		mockProductInfoService(sales, productOption, product);
//
// 		CreateOrderResponse createOrderResponse = orderService.createOrder(1L, request);
// 		Long orderId = createOrderResponse.order().orderId();
//
// 		//when
// 		OrderResponse orderDetail = orderService.getOrderDetail(1L, orderId);
//
// 		//then
// 		assertThat(orderDetail.orderId()).isEqualTo(orderId);
// 		assertThat(orderDetail.orderNumber()).isEqualTo(createOrderResponse.order().orderNumber());
// 		assertThat(orderDetail.orderStatus()).isEqualTo(OrderStatus.CREATED);
// 		assertThat(orderDetail.orderTime()).isEqualToIgnoringNanos(createOrderResponse.order().orderTime());
// 		assertThat(orderDetail.totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(50000));
// 		assertThat(orderDetail.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(50000));
// 		assertThat(orderDetail.usePoint()).isEqualTo(0);
//
// 		List<OrderItemResponse> items = orderDetail.orderItems();
// 		assertThat(items).hasSize(1);
//
// 		OrderItemResponse orderItem = items.get(0);
// 		assertThat(orderItem.orderItemId()).isNotNull();
// 		assertThat(orderItem.saleId()).isEqualTo(sales.getId());
// 		assertThat(orderItem.productOptionId()).isEqualTo(productOption.getId());
// 		assertThat(orderItem.orderItemName()).isEqualTo(product.getName());
// 		assertThat(orderItem.orderItemThumbnail()).isEqualTo(product.getThumbnail());
// 		assertThat(orderItem.orderItemPrice()).isEqualByComparingTo(BigDecimal.valueOf(50000));
// 		assertThat(orderItem.orderItemQuantity()).isEqualTo(1);
// 		assertThat(orderItem.orderItemAmount()).isEqualByComparingTo(BigDecimal.valueOf(50000));
// 		assertThat(orderItem.orderItemStatus()).isEqualTo(OrderItemStatus.CREATED);
// 	}
//
// 	@DisplayName("주문 목록 조회 시 기간과 상태 조건으로 필터링된 주문 목록을 조회합니다.")
// 	@Test
// 	void getOrderList(){
// 		//given
// 		Product product = createProduct();
// 		ProductOption productOption = createProductOption(product.getId());
//
// 		Sales sales = createSales(productOption.getId(), BigDecimal.valueOf(50000), 1, SalesStatus.ON_SALE);
//
// 		Cart cart = createCart(1L);
// 		createCartItem(cart.getId(), productOption.getId(), BigDecimal.valueOf(60000), 1);
//
// 		SalesItem salesItem = SalesItem.of(1L, sales.getId(), 1, BigDecimal.valueOf(45000));
//
// 		CreateOrderRequest request1 = CreateOrderRequest.fromCart("idem-key-001", cart.getId());
// 		CreateOrderRequest request2 = CreateOrderRequest.fromSalesItem("idem-key-002", salesItem);
// 		CreateOrderRequest request3 = CreateOrderRequest.fromDirect("idem-key-003", productOption.getId());
//
// 		mockProductInfoService(sales, productOption, product);
//
// 		CreateOrderResponse createdOrder1 = orderService.createOrder(1L, request1);
// 		CreateOrderResponse createdOrder2 = orderService.createOrder(1L, request2);
// 		CreateOrderResponse createdOrder3 = orderService.createOrder(1L, request3);
//
// 		List<CreateOrderResponse> createdOrders = List.of(createdOrder3, createdOrder2, createdOrder1);
//
// 		LocalDate today = LocalDate.now();
//
// 		//when
// 		ReadOrderListResponse response = orderService.getOrderList(
// 			1L, today, today, null, PageRequest.of(0, 10)
// 		);
//
// 		//then
// 		assertThat(response.orders()).hasSize(3);
// 		List<OrderResponse> orders = response.orders();
// 		IntStream.range(0, orders.size()).forEach(i -> {
// 			OrderResponse createdOrder = createdOrders.get(i).order();
// 			assertThat(orders.get(i).orderId()).isEqualTo(createdOrder.orderId());
// 			assertThat(orders.get(i).orderNumber()).isEqualTo(createdOrder.orderNumber());
// 			assertThat(orders.get(i).orderStatus()).isEqualTo(OrderStatus.CREATED);
// 			assertThat(orders.get(i).orderTime()).isEqualToIgnoringNanos(createdOrder.orderTime());
// 			assertThat(orders.get(i).totalQuantity()).isEqualTo(1);
// 			assertThat(orders.get(i).totalPrice()).isEqualByComparingTo(createdOrder.totalPrice());
// 			assertThat(orders.get(i).totalAmount()).isEqualByComparingTo(createdOrder.totalAmount());
// 			assertThat(orders.get(i).usePoint()).isEqualTo(0);
// 			assertThat(orders.get(i).orderItems()).hasSize(1);
// 			assertThat(orders.get(i).orderItems().get(0).orderItemId()).isNotNull();
// 			assertThat(orders.get(i).orderItems().get(0).saleId()).isEqualTo(sales.getId());
// 			assertThat(orders.get(i).orderItems().get(0).productOptionId()).isEqualTo(productOption.getId());
// 		});
//
// 	}
//
// 	@DisplayName("주문 목록 조회 시 시작일이 종료일보다 늦으면 예외가 발생합니다.")
// 	@Test
// 	void getOrderListWithInvalidDateRange(){
// 		//given
// 		Long memberId = 1L;
// 		LocalDate startDate = LocalDate.now();
// 		LocalDate endDate = LocalDate.now().minusDays(1);
// 		//when //then
// 		assertThatThrownBy(() -> orderService.getOrderList(memberId, startDate, endDate, null, PageRequest.of(0, 10) )).isInstanceOf(BusinessException.class).hasMessage("시작일은 종료일보다 빠르거나 같아야 합니다.");
// 	}
//
// 	@DisplayName("주문 상태를 변경하면 주문과 주문 이력이 함께 반영됩니다.")
// 	@Test
// 	void updateOrderStatus(){
// 		//given
// 		Product product = createProduct();
// 		ProductOption productOption = createProductOption(product.getId());
// 		Sales sales = createSales(productOption.getId(), BigDecimal.valueOf(50000), 1, SalesStatus.ON_SALE);
// 		Cart cart = createCart(1L);
// 		createCartItem(cart.getId(), productOption.getId(), BigDecimal.valueOf(60000), 1);
// 		CreateOrderRequest request = CreateOrderRequest.fromCart("idem-key-001", cart.getId());
//
// 		mockProductInfoService(sales, productOption, product);
//
// 		CreateOrderResponse created = orderService.createOrder(1L, request);
//
// 		Long orderId = created.order().orderId();
// 		String clientRequestId = "idem-key-002";
// 		//when
// 		orderService.updateOrderStatus(orderId, OrderStatus.PAYMENT_PENDING, clientRequestId);
//
// 		//then
// 		Order updated = orderRepository.findById(orderId).orElseThrow();
// 		assertThat(updated.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
// 		assertThat(orderHistoryRepository.findByOrderIdAndIdempotencyKey(orderId, clientRequestId).orElseThrow().getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
// 	}
//
// 	@DisplayName("같은 멱등성 키와 상태로 두 번 호출하면 아무 변화가 없습니다.")
// 	@Test
// 	void updateOrderStatusSameIdempotencyKeyAndStatus() {
// 		// given
// 		Product product = createProduct();
// 		ProductOption productOption = createProductOption(product.getId());
// 		Sales sales = createSales(productOption.getId(), BigDecimal.valueOf(50000), 1, SalesStatus.ON_SALE);
// 		Cart cart = createCart(1L);
// 		createCartItem(cart.getId(), productOption.getId(), BigDecimal.valueOf(60000), 1);
// 		CreateOrderRequest request = CreateOrderRequest.fromCart("idem-key-001", cart.getId());
// 		mockProductInfoService(sales, productOption, product);
// 		CreateOrderResponse created = orderService.createOrder(1L, request);
//
// 		String clientRequestId = "idem-key-002";
//
// 		Long orderId = created.order().orderId();
// 		orderService.updateOrderStatus(orderId, OrderStatus.PAYMENT_PENDING, clientRequestId);
// 		long historyCountAfterFirst = orderHistoryRepository.count();
//
// 		// when
// 		orderService.updateOrderStatus(orderId, OrderStatus.PAYMENT_PENDING, clientRequestId);
// 		long historyCountAfterSecond = orderHistoryRepository.count();
//
// 		// then
// 		assertThat(historyCountAfterSecond).isEqualTo(historyCountAfterFirst);
// 	}
//
// 	@DisplayName("같은 멱등성 키로 다른 상태를 요청하면 예외가 발생합니다.")
// 	@Test
// 	void updateOrderStatusWithDifferentIdempotencyKeyAndStatus(){
// 		//given
// 		Product product = createProduct();
// 		ProductOption productOption = createProductOption(product.getId());
// 		Sales sales = createSales(productOption.getId(), BigDecimal.valueOf(50000), 1, SalesStatus.ON_SALE);
// 		Cart cart = createCart(1L);
// 		createCartItem(cart.getId(), productOption.getId(), BigDecimal.valueOf(60000), 1);
// 		String clientRequestId = "idem-key-001";
// 		CreateOrderRequest request = CreateOrderRequest.fromCart(clientRequestId, cart.getId());
//
// 		mockProductInfoService(sales, productOption, product);
// 		CreateOrderResponse created = orderService.createOrder(1L, request);
// 		Long orderId = created.order().orderId();
//
// 		//when //then
// 		assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, OrderStatus.PAYMENT_PENDING, clientRequestId)).isInstanceOf(BusinessException.class).hasMessage(
// 			CommonErrorCode.IDEMPOTENCY_CONFLICT.getMessage());
// 	}
//
// 	@DisplayName("주문에 속한 모든 주문 상품 상태를 변경합니다.")
// 	@Test
// 	void updateAllOrderItemsStatus(){
// 		//given
// 		Product product = createProduct();
// 		ProductOption productOption = createProductOption(product.getId());
// 		Sales sales = createSales(productOption.getId(), BigDecimal.valueOf(50000), 1, SalesStatus.ON_SALE);
// 		Cart cart = createCart(1L);
// 		createCartItem(cart.getId(), productOption.getId(), BigDecimal.valueOf(60000), 1);
// 		CreateOrderRequest request = CreateOrderRequest.fromCart("idem-key-001", cart.getId());
// 		mockProductInfoService(sales, productOption, product);
// 		CreateOrderResponse created = orderService.createOrder(1L, request);
// 		Long orderId = created.order().orderId();
//
// 		List<OrderItem> before = orderItemRepository.findByOrderId(orderId);
//
// 		//when
// 		orderService.updateAllOrderItemsStatus(orderId, OrderItemStatus.PAID);
//
// 		//then
// 		List<OrderItem> after = orderItemRepository.findByOrderId(orderId);
// 		assertThat(after).hasSize(before.size());
// 		after.forEach(item -> assertThat(item.getOrderItemStatus()).isEqualTo(OrderItemStatus.PAID));
// 	}
//
// 	@DisplayName("한 개의 주문 상품 상태를 변경하면 해당 주문 상품만 상태가 변경됩니다.")
// 	@Test
// 	void updateOrderItemStatus(){
// 		//given
// 		Product product = createProduct();
// 		ProductOption productOption = createProductOption(product.getId());
// 		Sales sales = createSales(productOption.getId(), BigDecimal.valueOf(50000), 1, SalesStatus.ON_SALE);
// 		Cart cart = createCart(1L);
// 		createCartItem(cart.getId(), productOption.getId(), BigDecimal.valueOf(60000), 1);
// 		CreateOrderRequest request = CreateOrderRequest.fromCart("idem-key-001", cart.getId());
// 		mockProductInfoService(sales, productOption, product);
// 		CreateOrderResponse created = orderService.createOrder(1L, request);
// 		Long orderId = created.order().orderId();
// 		Long orderItemId = created.order().orderItems().get(0).orderItemId();
//
// 		//when
// 		orderService.updateOrderItemStatus(orderId, orderItemId, OrderItemStatus.PAID);
//
// 		//then
// 		assertThat(orderItemRepository.findById(orderItemId).orElseThrow().getOrderItemStatus()).isEqualTo(OrderItemStatus.PAID);
// 	}
// }