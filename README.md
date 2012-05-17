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

### Parsing the response

```java
Http.get("http://foo.com").{responseCode, asString, asBytes, asParams}
```

## Installation

### sbt

```scala
import sbt._
object build extends Build {
  lazy val root = Project("root", file(".")) dependsOn(
    uri("git://github.com/xuwei-k/javaj-http.git#version")
  )
}
```

