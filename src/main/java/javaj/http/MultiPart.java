package javaj.http;

import javaj.http.*;
import lombok.Getter;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MultiPart {
  public static MultiPart apply(String name ,String filename ,String mime ,String data ){
    try{
      return new MultiPart(name, filename, mime, data.getBytes(Http.charset));
    }catch(java.io.UnsupportedEncodingException e){
      throw new Error(e);
    }
  }

  public final String name;
  public final String filename;
  public final String mime;
  public final byte[] data;
}

