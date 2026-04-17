package edu.lab.core.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends LabException {

	public UnauthorizedException(String message) {
		super(40100, HttpStatus.UNAUTHORIZED, message);
	}
}