package javaj.http;

import lombok.Getter;
import lombok.AllArgsConstructor;
import fj.*;

@AllArgsConstructor
public final class Token extends P2<String,String>{
  public final String key;
  public final String secret;
  public String _1(){ return key;}
  public String _2(){ return secret;}
}

