name := "generic-monadic-services"

libraryDependencies ++= Seq(
  "org.apache.commons" % "commons-io" % "1.3.2" % "test",
  "commons-codec" % "commons-codec" % "1.10" % "test",
  "org.jasypt" % "jasypt" % "1.9.2" % "test",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "org.scalaz" %% "scalaz-core" % "7.2.2" % "test",
  "org.scalatest" %% "scalatest" % "2.2.0" % "test"
)
