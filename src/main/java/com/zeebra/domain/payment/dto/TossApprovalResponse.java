package com.zeebra.domain.payment.dto;

public record TossApprovalResponse(TossPayment tossPayment, TossError tossError) {
	public static TossApprovalResponse ofSuccess(TossPayment tossPayment) {
		return new TossApprovalResponse(tossPayment, null);
	}
	public static TossApprovalResponse ofFail(TossError tossError) {
		return new TossApprovalResponse(null, tossError);
	}
}