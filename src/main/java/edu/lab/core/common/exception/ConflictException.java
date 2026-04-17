package edu.lab.core.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends LabException {

	public ConflictException(String message) {
		super(40900, HttpStatus.CONFLICT, message);
	}
}