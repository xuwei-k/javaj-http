package javaj.http;

import java.net.URL;
import java.net.URI;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import fj.*;
import fj.data.List;
import fj.data.Option;
import lombok.val;
import static fj.P.p;

public final class OAuth {
  public static final String MAC = "HmacSHA1";

  public static final <A> String mkString(final List<A> list,final String separator){
    if(list.isEmpty()){
      return "";
    }else{
      return
      list.map(
        new F<A,String>(){
          public String f(final A e){
            return e.toString();
          }
        }
      ).foldLeft1(
        new F2<String,String,String>(){
          public String f(String s,String e){
            return s + separator + e;
          }
        }
      );
    }
  }

  public static Http.Request sign(Http.Request req,Token consumer,Option<Token> token,Option<String> verifier){

    final List<P2<String,String>> baseParams = List.list(
      p("oauth_timestamp", String.valueOf(System.currentTimeMillis()/1000)),
      p("oauth_nonce", String.valueOf(System.currentTimeMillis()))
    );

    final P2<List<P2<String,String>>,String> s = getSig(baseParams, req, consumer, token, verifier);
    final List<P2<String,String>> oauthParams = s._1().cons(p("oauth_signature",s._2()));

    return
    req.header("Authorization", "OAuth " +
      mkString(
        oauthParams.map(
          new F<P2<String,String>,String>(){
            public String f(P2<String,String> p){
              return p._1() + "=\"" + percentEncode.f(p._2()) +"\"";
            }
          }
        )
      ,",")
    );
  }

  public static P2<List<P2<String,String>>,String> getSig(List<P2<String,String>> baseParams,Http.Request req,Token consumer,Option<Token> token,Option<String> verifier){
    final List<P2<String,String>> oauthParams =
      List.<P2<String,String>>list(
        p("oauth_version", "1.0"),p("oauth_consumer_key", consumer.key),p("oauth_signature_method", "HMAC-SHA1")
      ).append(
        baseParams
      ).append(
        token.toList().map(
          new F<Token,P2<String,String>>(){
            public P2<String,String> f(Token t){
              return p("oauth_token", t.key);
            }
          }
        )
      ).append(
        verifier.toList().map(
          new F<String,P2<String,String>>(){
            public P2<String,String> f(String v){
              return p("oauth_verifier", v);
            }
          }
        )
      );

    final String baseString = mkString(
      List.list(
        req.method.toUpperCase(),normalizeUrl(req.url.f(req)),normalizeParams(req.params.append(oauthParams))
      ).map(percentEncode)
    ,"&");

    val keyString = percentEncode.f(consumer.secret) + "&" + token.map(
      new F<Token,String>(){
        public String f(Token t){
          return percentEncode.f(t.secret);
        }
      }
    ).orSome("");

    try{
      val key = new SecretKeySpec(keyString.getBytes(Http.charset), MAC);
      val mac = Mac.getInstance(MAC);
      mac.init(key);
      final byte[] text = baseString.getBytes(Http.charset);
      return p(oauthParams, Http.base64(mac.doFinal(text)));
    }catch(Exception e){
      throw new Error(e);
    }
  }

  private static String normalizeParams(List<P2<String,String>> params){
    return mkString(percentEncode(params).sort(Ord.stringOrd),"&");
  }

  private static String normalizeUrl(URL url){
    try{
      val uri = new URI(url.toString());
      val scheme = uri.getScheme().toLowerCase();
      String authority = uri.getAuthority().toLowerCase();
      val dropPort = (scheme.equals("http") && uri.getPort() == 80) || (scheme.equals("https") && uri.getPort() == 443);
      if (dropPort) {
        // find the last : in the authority
        val index = authority.lastIndexOf(":");
        if (index >= 0) {
          authority = authority.substring(0, index);
        }
      }
      String path = uri.getRawPath();
      if (path == null || path.length() <= 0) {
        path = "/"; // conforms to RFC 2616 section 3.2.2
      }
      // we know that there is no query and no fragment here.
      return scheme + "://" + authority + path;
    }catch(java.net.URISyntaxException e){
      throw new Error(e);
    }
  }

  public static List<String> percentEncode(List<P2<String,String>> params){
    return params.map(
      new F<P2<String,String>,String>(){
        public String f(P2<String,String> p){
          return percentEncode.f(p._1()) + "=" + percentEncode.f(p._2());
        }
      }
    );
  }

  public static final F<String,String> percentEncode = new F<String,String>(){
    public String f(final String s){
      if (s == null){
        return "";
      }else {
        return Http.urlEncode(s).replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
      }
    }
  };
}
