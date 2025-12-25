package com.example.demo.exception;

public class ExcelInUseException extends RuntimeException {

	public ExcelInUseException(String message) {
		super(message);
	}

	public ExcelInUseException(String message, Throwable cause) {
		super(message, cause);
	}
}
