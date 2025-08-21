package jp.co.kpscorp.rc6.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotfoundException extends RuntimeException {
	private static final long serialVersionUID = 3410779274712533375L;

	public NotfoundException(String message) {
		super(message);
	}

}
