package javaj.http;

import javaj.http.*;
import lombok.Getter;

class HttpException extends RuntimeException{
  public HttpException(int code , String message ,String body ){
    super(code + ": " + message);
    this.code    = code;
    this.message = message;
    this.body    = body;
  }
  @Getter public final int code;
  @Getter public final String message;
  @Getter public final String body;
}

