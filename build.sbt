name := "javaj-http"

version := "0.1-SNAPSHOT"

organization := "com.github.xuwei-k"

scalaVersion := "2.9.2"

libraryDependencies ++= Seq(
  "commons-codec"        % "commons-codec"      % "1.5",
  "junit"                % "junit"              % "4.10"          % "test",
  "com.novocode"         % "junit-interface"    % "0.8"           % "test",
  "org.functionaljava"   % "functionaljava"     % "3.0",
  "org.projectlombok"    % "lombok"             % "0.11.0"
)

autoScalaLibrary := false

homepage := Some(url("https://github.com/xuwei-k/javaj-http"))

publishTo := sys.env.get("MAVEN_DIRECTORY").map{ dir =>
  Resolver.file("gh-pages",file(dir))(Patterns(true, Resolver.mavenStyleBasePattern))
}

pomIncludeRepository := { x => false }

publishArtifact in Test := false

crossPaths := false

licenses := Seq("Apache 2" -> url("https://github.com/xuwei-k/javaj-http/blob/master/LICENSE.txt"))

pomExtra := (
  <licenses>
    <license>
      <name>Apache 2</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:xuwei-k/javaj-http.git</url>
    <connection>scm:git:git@github.com:xuwei-k/javaj-http.git</connection>
  </scm>
)
