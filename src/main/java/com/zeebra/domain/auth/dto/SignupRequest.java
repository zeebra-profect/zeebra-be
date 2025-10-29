package com.zeebra.domain.auth.dto;

import java.time.LocalDate;

import com.zeebra.domain.member.entity.Gender;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
	@NotBlank String userLoginId,
	@NotBlank String memberName,
	@NotBlank @Email String memberEmail,
	@NotBlank String nickname,
	@NotBlank @Size(min = 8, max = 20)   @Pattern(
		regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+=-]).+$",
		message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다."
	) String password,
	@NotBlank String confirmPassword,
	@NotNull LocalDate memberBirth,
	@NotNull Gender memberGender
) {
	@AssertTrue(message = "비밀번호와 확인 비밀번호가 일치하지 않습니다.")
	public boolean isPasswordConfirmed() {
		return password != null && password.equals(confirmPassword);
	}
}