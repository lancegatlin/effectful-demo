scalaVersion := "2.12.2"

organization := "org.lancegatlin"

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

val consoleDisabledScalacOpts = Set(
  "-Ywarn-value-discard",
  "-Ywarn-dead-code",
  "-Ywarn-unused",
  "-Ywarn-unused-import",
  "-Xlint",
  "-Xfatal-warnings"
)

scalacOptions in (Compile,console) ~= (_ filterNot consoleDisabledScalacOpts )
scalacOptions in (Test,console) ~= (_ filterNot consoleDisabledScalacOpts )