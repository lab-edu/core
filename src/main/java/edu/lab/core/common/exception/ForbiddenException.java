package edu.lab.core.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends LabException {

	public ForbiddenException(String message) {
		super(40300, HttpStatus.FORBIDDEN, message);
	}
}