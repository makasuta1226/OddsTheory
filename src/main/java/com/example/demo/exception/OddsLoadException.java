package com.example.demo.exception;

public class OddsLoadException extends RuntimeException {

	public OddsLoadException(String message) {
		super(message);
	}

	public OddsLoadException(String message, Throwable cause) {
		super(message, cause);
	}
}
