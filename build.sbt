name := "GitHub Project Relations Visualization"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.9.1"

scalacOptions += "-deprecation"

seq(webSettings :_*)

classpathTypes ~= (_ + "orbit")

resolvers ++= Seq(
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.1",
  "org.scalaz" %% "scalaz-core" % "6.0.4",
  "net.van-antwerpen" %% "scala-collection-mapreduce" % "0.1.0-SNAPSHOT",
  "org.scalatra" % "scalatra" % "2.1.1",
  "org.scalatra" % "scalatra-scalate" % "2.1.1",
  "net.liftweb" %% "lift-json" % "2.5-M1",
  "net.liftweb" %% "lift-json-ext" % "2.5-M1",
  "ch.qos.logback" % "logback-classic" % "1.0.6" % "runtime",
  "org.eclipse.jetty" % "jetty-webapp" % "8.1.7.v20120910" % "container",
  "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" % "container;provided;test" artifacts (Artifact("javax.servlet", "jar", "jar")),
  "org.scalatest" %% "scalatest" % "1.8" % "test",
  "com.typesafe.akka" % "akka-actor" % "2.0.2"
)

compile <<= (resourceDirectory in Compile, compile in Compile) map { (rd,result) =>
  val f = rd / "/commits.txt"
  if ( !(f exists) ) throw new Exception("Please copy commits.txt to "+f)
  result
}

fork in run := true

//connectInput in run := true

javaOptions ++=Seq(
"-XX:+UseParallelGC",
"-Xms2048m",
"-Xmx2048m"
)
