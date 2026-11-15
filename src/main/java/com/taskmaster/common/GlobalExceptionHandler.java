package com.taskmaster.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(ApiException.class)
	ProblemDetail handleApiException(ApiException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.status(), exception.getMessage());
		problem.setTitle(exception.status().getReasonPhrase());
		return problem;
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleConflict(DataIntegrityViolationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.CONFLICT,
				"Request conflicts with existing data");
		problem.setTitle(HttpStatus.CONFLICT.getReasonPhrase());
		return problem;
	}

	@Override
	protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
			MaxUploadSizeExceededException exception,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Attachment exceeds the 10 MB limit");
		problem.setTitle(HttpStatus.CONTENT_TOO_LARGE.getReasonPhrase());
		return handleExceptionInternal(exception, problem, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException exception,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
		problem.setTitle(HttpStatus.BAD_REQUEST.getReasonPhrase());
		Map<String, String> errors = new LinkedHashMap<>();
		exception.getBindingResult().getFieldErrors()
				.forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
		problem.setProperty("errors", errors);
		return handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
	}
}
