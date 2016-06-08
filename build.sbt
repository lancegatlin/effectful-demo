name := "effectful-demo"

libraryDependencies ++= Seq(
  "org.apache.commons" % "commons-io" % "1.3.2" % "test",
  "commons-codec" % "commons-codec" % "1.10" % "test",
  "org.jasypt" % "jasypt" % "1.9.2" % "test",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "org.scalaz" %% "scalaz-core" % "7.2.2" % "test",
  "org.scalatest" %% "scalatest" % "2.2.0" % "test",
  "com.mchange" % "c3p0" % "0.9.5.2" % "test",
  "net.s_mach" %% "concurrent" % "1.1.0" % "test",
  "com.h2database" % "h2" % "1.4.192"
)

scalacOptions ++= Seq(
  "-feature",
  "-unchecked",
  "-deprecation",
//  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
//  "-Ywarn-unused-import",
  "-Xfatal-warnings",
  "-Xlint",
  "-language:higherKinds"
)
