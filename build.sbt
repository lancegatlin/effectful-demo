name := "effectful-demo"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats" % "0.9.0",
  "io.monadless" %% "monadless-stdlib" % "0.0.13",
  "io.monadless" %% "monadless-cats" % "0.0.13",
  "ch.qos.logback" % "logback-classic" % "1.1.7"
)

libraryDependencies ++= Seq(
  "org.apache.commons" % "commons-io" % "1.3.2" % "test",
  "commons-codec" % "commons-codec" % "1.10" % "test",
  "org.jasypt" % "jasypt" % "1.9.2" % "test",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "com.mchange" % "c3p0" % "0.9.5.2" % "test",
  "net.s_mach" %% "concurrent" % "2.0.0" % "test",
  "com.h2database" % "h2" % "1.4.192" % "test"
)
