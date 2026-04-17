package edu.lab.core.common.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends LabException {

	public BadRequestException(String message) {
		super(40000, HttpStatus.BAD_REQUEST, message);
	}
}