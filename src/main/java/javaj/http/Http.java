package javaj.http;

import lombok.val;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.ToString;
import fj.*;
import fj.data.*;
import static fj.P.p;
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

public final class Http {

  public final static <A> List<A> array2List(final A[] a){
    final F<A[],Array<A>> f = Array.wrap();
    return f.f(a).toList();
  }

  public abstract static class HttpExec extends F2<Request,HttpURLConnection,Unit>{
    public final Unit f(Request r,HttpURLConnection c){
      try{
        e(r,c);
        return null;
      }catch(Exception e){
        throw new Error(e);
      }
    }
    abstract void e(Request r,HttpURLConnection c) throws Exception;
  }
  public abstract static class HttpUrl extends F<Request,URL>{
    public final URL f(Request r){
      try{
        return e(r);
      }catch(Exception e){
        throw new Error(e);
      }
    }
    abstract URL e(Request r) throws Exception;
  }

  @ToString
  @AllArgsConstructor
  public static final class Request {
    private static final List<P2<String,String>> nil = List.nil();
    public static Request apply(HttpExec exec ,HttpUrl url ,String method){
      return new Request(method, exec, url, nil, nil, defaultOptions.cons(HttpOptions.method(method)));
    }

    public final String                       method;
    public final HttpExec                     exec;
    public final HttpUrl                      url;
    public final List<P2<String,String>>      params;
    public final List<P2<String,String>>      headers;
    public final List<HttpOptions.HttpOption> options;

    public Request params(P2<String, String>... p){
      return params(array2List(p));
    }
    public Request params(List<P2<String,String>> p){
      return new Request(method, exec,url, p, headers,options);
    }
    public Request headers(P2<String,String>... h){
      return headers(array2List(h));
    }
    public Request headers(List<P2<String,String>> h){
      return new Request(method,exec,url, params, headers.append(h),options);
    }
    public Request param(String key,String value){
      return new Request(method,exec,url,params.cons(p(key,value)),headers,options);
    }
    public Request header(String key,String value){
      return new Request(method,exec,url,params, headers.cons(p(key,value)),options);
    }
    public Request options(HttpOptions.HttpOption... o){
      return options(array2List(o));
    }
    public Request options(List<HttpOptions.HttpOption> o){
      return new Request(method,exec, url, params, headers, o.append(options));
    }
    public Request option(HttpOptions.HttpOption o){
      return new Request(method,exec,url, params, headers,options.cons(o));
    }
    public Request auth(String user,String password) {
      return header("Authorization", "Basic " + base64(user + ":" + password));
    }
    public Request oauth(Token consumer){
      final Option<Token>  t = Option.none();
      final Option<String> s = Option.none();
      return oauth(consumer, t , s );
    }
    public Request oauth(Token consumer,Token token){
      final Option<Token>  t = Option.some(token);
      final Option<String> s = Option.none();
      return oauth(consumer, t , s );
    }
    public Request oauth(Token consumer,Token token,String verifier){
      return oauth(consumer, Option.some(token), Option.some(verifier));
    }
    public Request oauth(Token consumer,Option<Token> token,Option<String> verifier ){
      return OAuth.sign(this, consumer, token, verifier);
    }

    public URL getUrl(){ return url.f(this); };

    public <T> T apply(final F<InputStream,T> parser )throws Exception{
      return process(
        new F<HttpURLConnection,T>(){
          public T f(HttpURLConnection conn){
            try{
              return tryParse(conn.getInputStream(), parser);
            }catch(IOException e){
              throw new Error(e);
            }
          }
        }
      );
    }

    public <T> T process(final F<HttpURLConnection,T> processor )throws Exception{

      final URLConnection c = url.f(this).openConnection();
      if(c != null || c instanceof HttpURLConnection){
        final HttpURLConnection conn = (HttpURLConnection)c;
        conn.setInstanceFollowRedirects(true);
        for(val h:headers.reverse()){
          conn.setRequestProperty(h._1(),h._2());
        }
        for(val o:options.reverse()){
          o.e(conn);
        }

        exec.f(this, conn);
        try {
          return processor.f(conn);
        } catch(Exception e) {
          throw new HttpException(
            conn.getResponseCode(),
            conn.getResponseMessage(),
            tryParse(conn.getErrorStream(),readString)
          );
        }
      }else{
        throw new Exception(c + " is not HttpURLConnection");
      }
    }

    public int responseCode() throws Exception{
      return process(
        new F<HttpURLConnection,Integer>(){
          public Integer f(HttpURLConnection conn){
            try{
              return conn.getResponseCode();
            }catch(Exception e){
              throw new Error(e);
            }
          }
        }
      );
    }

    public byte[] asBytes() throws Exception{ return apply(readBytes); }

    public String asString() throws Exception{ return apply(readString); }

// TODO java xml library ...
//    public asXml = apply(is => scala.xml.XML.load(is));

    public List<P2<String,String>> asParams() throws Exception {
      return
      array2List(asString().split("&")).bind(
        new F<String,List<P2<String,String>>>(){
          public List<P2<String,String>> f(final String s){
            final String[] a = s.split("=");
            if(a.length == 2){
              return List.list(p(urlDecode(a[0]), urlDecode(a[1])));
            }else{
              return List.nil();
            }
          }
        }
      );
    }

    public HashMap<String,String> asParamMap() throws Exception {
      final HashMap<String,String> map = HashMap.hashMap();
      for(val p:asParams()){
        map.set(p._1(),p._2());
      }
      return map;
    };

    public Token asToken() throws Exception {
      final HashMap<String,String> params = asParamMap();
      return new Token(params.get("oauth_token").some(),params.get("oauth_token_secret").some());
    }
  }

  public static <E> E tryParse(InputStream is ,F<InputStream,E> parser) throws IOException{
    try {
      return parser.f(is);
    } finally {
      is.close();
    }
  }

  /**
   * [lifted from lift]
   */
  public static final F<InputStream,String> readString = new F<InputStream,String>(){
    public String f(InputStream is){
      try{
        final InputStreamReader in = new InputStreamReader(is, charset);
        final StringBuilder bos = new StringBuilder();
        final char[] ba = new char[4096];

        int len;
        while((len = in.read(ba)) > 0){
          bos.append(ba, 0, len);
        }
        return bos.toString();
      }catch(Exception e){
        throw new Error(e);
      }
    }
  };

  /**
   * [lifted from lift]
   * Read all data from a stream into an Array[Byte]
   */
  public static F<InputStream,byte[]> readBytes = new F<InputStream,byte[]>(){
    public byte[] f(final InputStream in){
      try{
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final byte[] ba = new byte[4096];
        int len;
        while((len = in.read(ba)) > 0){
          bos.write(ba, 0, len);
        }
        return bos.toByteArray();
      }catch(Exception e){
        throw new Error(e);
      }
    }
  };

  public static final List<HttpOptions.HttpOption> defaultOptions = List.list(HttpOptions.connTimeout(50000), HttpOptions.readTimeout(50000));

  public static String urlEncode(String name){
    try{
      return URLEncoder.encode(name, charset);
    }catch(UnsupportedEncodingException e){
      throw new Error(e);
    }
  }
  public static String urlDecode(String name){
    try{
      return URLDecoder.decode(name, charset);
    }catch(UnsupportedEncodingException e){
      throw new Error(e);
    }
  }
  public static String base64(byte[] bytes ){ return new String(Base64.encodeBase64(bytes)); };
  public static String base64(String in){
    try{
      return base64(in.getBytes(charset));
    }catch(UnsupportedEncodingException e){
      throw new Error(e);
    }
  }

  public static String toQs(final List<P2<String,String>> params){
    return OAuth.mkString(
      params.map(new F<P2<String,String>,String>(){
        public String f(P2<String,String> p){
          return urlEncode(p._1()) + "=" + urlEncode(p._2());
        }
      })
    ,"&");
  }

  public static String appendQs(String url,List<P2<String,String>> params){
    return
      url +
      (params.isEmpty() ?  "" : ( url.contains("?") ? "&" : "?" ) ) +
      toQs(params);
  }

  public static HttpUrl appendQsHttpUrl(final String url ){
    return new HttpUrl(){
      URL e(Request r) throws Exception{
        return new URL(appendQs(url, r.params));
      }
    };
  }
  public static HttpUrl noopHttpUrl(final String url){
    return new HttpUrl(){
      URL e(Request r) throws Exception{
        return new URL(url);
      }
    };
  }

  public static Request get(String url){
    final HttpExec getFunc = new HttpExec(){
      void e(Request req,HttpURLConnection conn) throws Exception{
        conn.connect();
      }
    };
    return Request.apply(getFunc, appendQsHttpUrl(url), "GET");
  }

  public static final String CrLf = "\r\n";
  public static final String Pref = "--";
  public static final String Boundary = "gc0pMUlT1B0uNdArYc0p";

  public static Request multipart(final String url ,final MultiPart... parts ){
    final Http.HttpExec postFunc = new HttpExec(){
      void e(Request req,HttpURLConnection conn)throws Exception{
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + Boundary);
        conn.setRequestProperty("MIME-Version", "1.0");

        val out = new DataOutputStream(conn.getOutputStream());

        for(val p:req.params){
          final String name = p._1();
          final String value = p._2();
          out.writeBytes(Pref + Boundary + CrLf);
          out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"");
          out.writeBytes(CrLf + CrLf + value.toString() + CrLf);
        }

        for(val part:parts){
          out.writeBytes(Pref + Boundary + CrLf);
          out.writeBytes("Content-Disposition: form-data; name=\"" + part.name + "\"; filename=\"" + part.filename + "\"" + CrLf);
          out.writeBytes("Content-Type: " + part.mime + CrLf + CrLf);
          out.write(part.data);
          out.writeBytes(CrLf + Pref + Boundary + Pref + CrLf);
        }
        out.flush();
        out.close();
      }
    };
    return Http.Request.apply(postFunc, Http.noopHttpUrl(url), "POST");
  }

  public static Request postData(String url,String data){
    try{
      return postData(url, data.getBytes(charset));
    }catch(Exception e){
      throw new Error(e);
    }
  }
  public static Request postData(final String url,final byte[] data){
    final HttpExec postFunc = new HttpExec(){
      void e(Request req,HttpURLConnection conn) throws Exception{
        conn.setDoOutput(true);
        conn.connect();
        conn.getOutputStream().write(data);
      }
    };
    return Request.apply(postFunc, noopHttpUrl(url), "POST");
  }

  public static Request post(final String url){
    final HttpExec postFunc = new HttpExec(){
      void e(Request req,HttpURLConnection conn) throws Exception {
        conn.setDoOutput(true);
        conn.connect();
        conn.getOutputStream().write(toQs(req.params).getBytes(charset));
      }
    };
    return Request.apply(postFunc, noopHttpUrl(url), "POST").header("content-type", "application/x-www-form-urlencoded");
  }
  public static final String charset = "UTF-8";
}
