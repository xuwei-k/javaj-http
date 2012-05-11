package javaj.http;

import lombok.val;
import lombok.Getter;
import fj.*;
import fj.data.*;
import java.net.*;
import java.io.*;
import org.apache.commons.codec.binary.Base64;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.HostnameVerifier;
import java.security.cert.X509Certificate;

public class HttpOptions {
//  type HttpOption = HttpURLConnection => Unit
  public static abstract class HttpOption extends Effect<HttpURLConnection>{
    public final void e(final HttpURLConnection c){
      try{
        f(c);
      }catch(Exception e){
        throw new Error(e);
      }
    }
    abstract void f(HttpURLConnection c)throws Exception;
  }

  public static HttpOption method(final String method){
    return new HttpOption(){
      public void f(final HttpURLConnection c) throws Exception{
        c.setRequestMethod(method);
      }
    };
  }
  public static HttpOption connTimeout(final int timeout){
    return new HttpOption(){
      public void f(final HttpURLConnection c){
        c.setConnectTimeout(timeout);
      }
    };
  }
  public static HttpOption readTimeout(final int timeout){
    return new HttpOption(){
      public void f(final HttpURLConnection c){
        c.setReadTimeout(timeout);
      }
    };
  }
  public static HttpOption allowUnsafeSSL(){
    return new HttpOption(){
      public void f(final HttpURLConnection c) throws Exception {
        if(c instanceof HttpsURLConnection){
          val co = (HttpsURLConnection)c;
          final HostnameVerifier hv = new HostnameVerifier() {
            public boolean verify(String urlHostName,SSLSession session){return true;}
          };
          co.setHostnameVerifier(hv);

          final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers(){ return null; }
            public void checkClientTrusted(X509Certificate[] certs,String authType){}
            public void checkServerTrusted(X509Certificate[] certs,String authType){}
          }};

          final SSLContext sc = SSLContext.getInstance("SSL");
          sc.init(null, trustAllCerts, new java.security.SecureRandom());
          co.setSSLSocketFactory(sc.getSocketFactory());
        }
      }
    };
  }
  public static HttpOption sslSocketFactory(final SSLSocketFactory sslSocketFactory){
    return new HttpOption(){
      public void f(final HttpURLConnection c) throws Exception {
        if(c instanceof HttpsURLConnection){
          final HttpsURLConnection httpsConn = (HttpsURLConnection)c;
          httpsConn.setSSLSocketFactory(sslSocketFactory) ;
        }
      }
    };
  }
}


