package edu.lab.core.common.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends LabException {

	public NotFoundException(String message) {
		super(40400, HttpStatus.NOT_FOUND, message);
	}
}