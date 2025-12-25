package com.example.demo.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

	// OddsLoadExceptionを捕捉してエラーページに遷移
	@ExceptionHandler(OddsLoadException.class)
	public String handleOddsLoadException(OddsLoadException ex, Model model) {
		model.addAttribute("errorMessage", ex.getMessage());
		return "error/odds_error";
	}

	// ExcelInUseExceptionを捕捉してエラーページに遷移
	@ExceptionHandler(ExcelInUseException.class)
	public String handleExcelInUseException(ExcelInUseException ex, Model model) {
		model.addAttribute("errorMessage", ex.getMessage());
		return "error/odds_error";
	}

	// その他の例外（例えばNullPointerExceptionなど）を捕捉する場合
	@ExceptionHandler(Exception.class)
	public String handleGenericException(Exception ex, Model model) {
		model.addAttribute("errorMessage", "予期しないエラーが発生しました: " + ex.getMessage());
		return "error/odds_error";
	}
}
