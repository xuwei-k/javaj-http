package javaj.http;

import javaj.http.*;
import fj.data.Either;

public class HttpException extends Exception{
  public HttpException(int code , String message ,Either<Exception,String> body ){
    super(code + ": " + message);
    this.code    = code;
    this.message = message;
    this.body    = body;
  }
  public final int code;
  public final String message;
  public final Either<Exception,String> body;
}

