package com.zeebra.domain.payment.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record TossPayment(
	String version,
	String paymentKey,
	String type,
	String orderId,
	String orderName,
	String mId,
	String currency,
	String method,
	BigDecimal totalAmount,
	BigDecimal balanceAmount,
	String status,
	String requestedAt,
	String approvedAt,
	Boolean useEscrow,
	BigDecimal suppliedAmount,
	BigDecimal vat,
	Boolean cultureExpense,
	BigDecimal taxFreeAmount,
	BigDecimal taxExemptionAmount,
	Cancel cancel,
	Boolean isPartialCancelable,
	Card card,
	String secret,
	VirtualAccount virtualAccount,
	MobilePhone mobilePhone,
	GiftCertificate giftCertificate,
	Transfer transfer,
	Map<String, String> metadata,
	Receipt receipt,
	Checkout checkout,
	EasyPay easyPay,
	String country,
	Failure failure,
	CashReceipt cashReceipt,
	List<CashReceipts> cashReceipts,
	Discount discount
) {
	public record Cancel(
		BigDecimal cancelAmount,
		String cancelReason,
		BigDecimal taxFreeAmount,
		BigDecimal taxExemptionAmount,
		BigDecimal refundableAmount,
		BigDecimal cardDiscountAmount,
		BigDecimal transferDiscountAmount,
		BigDecimal easyPayDiscountAmount,
		String canceledAt,
		String transactionKey,
		String receiptKey,
		String cancelStatus,
		String cancelRequestId
		){
		// public static Cancel of(){
		// 	return new Cancel();
		// }
	}

	public record Card(
		BigDecimal amount,
		String issuerCode,
		String acquirerCode,
		String number,
		Integer installmentPlanMonths,
		String approveNo,
		Boolean useCardPoint,
		String cardType,
		String ownerType,
		String acquireStatus,
		Boolean isInterestFree,
		String interestPayer
	) {

	}

	public record VirtualAccount(
		String accountType,
		String accountNumber,
		String bankCode,
		String customerName,
		String depositorName,
		String dueDate,
		String refundStatus,
		Boolean expired,
		String settlementStatus,
		RefundReceiveAccount refundReceiveAccount
	) {
		public record RefundReceiveAccount(
			String bankCode, String accountNumber, String holderName
		){}
	}

	public record MobilePhone(
		String customerMobilePhone, String settlementStatus, String receiptUrl
	){}

	public record GiftCertificate(String approveNo, String settlementStatus){
	}

	public record Transfer(String bankCode, String accountNumber, String settlementStatus) {
	}

	public record Receipt(String url){}

	public record Checkout(String url){}

	public record EasyPay(String provider, BigDecimal amount, BigDecimal discountAmount){}

	public record Failure(String code, String message){}

	public record CashReceipt(String type, String receiptKey, String issueNumber, String receiptUrl, BigDecimal amount, BigDecimal taxFreeAmount){}

	public  record CashReceipts(String receiptKey, String orderId, String orderName, String type, String issueNumber, String receiptUrl, String businessNumber, String transaction, BigDecimal amount, BigDecimal taxFreeAmount, String issueStatus, Failure failure, String customerIdentityNumber, String requestedAt){ }

	public record Discount(Integer amount){}
}