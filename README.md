# Javaj-Http [![Build Status](https://secure.travis-ci.org/xuwei-k/javaj-http.png)](http://travis-ci.org/xuwei-k/javaj-http)

## porting [Scalaj Http](https://github.com/scalaj/scalaj-http) java only

This is a bare bones http client for java which wraps HttpURLConnection

## Usage

### Simple Get

```java
import javaj.http.Http;
  
Http.get("http://foo.com/search").param("q","monkeys").asString();
```

### Simple Post

```java
import static fj.P.p;

Http.post("http://foo.com/add").params(p("name","jon"),p("age","29")).asString();
```

### OAuth Dance and Request

```java
import javaj.http.*;

Token consumer = new Token("key", "secret");
Token token = Http.get("http://foursquare.com/oauth/request_token").param("oauth_callback","oob").oauth(consumer).asTokan();

System.out.println("Go to http://foursquare.com/oauth/authorize?oauth_token=" + token.key);

String verifier = "***********************";

Token accessToken = Http.get("http://foursquare.com/oauth/access_token").oauth(consumer, token, verifier).asTokan();

System.out.println(Http.get("http://api.foursquare.com/v1/history.json").oauth(consumer, accessToken).asString());
```

### Parsing the response

```java
Http.get("http://foo.com").{responseCode, asString, asBytes, asParams}
```


## Installation

### sbt

```scala
resolvers += "xuwei-k repo" at "http://xuwei-k.github.com/mvn"

libraryDependencies += "com.github.xuwei-k" % "javaj-http" % "version"
```

or

```scala
import sbt._
object build extends Build {
  lazy val root = Project("root", file(".")) dependsOn(
    uri("git://github.com/xuwei-k/javaj-http.git#version")
  )
}

```

