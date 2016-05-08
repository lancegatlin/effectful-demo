resolvers ++= Seq(
  Classpaths.sbtPluginReleases,
  "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/",
  Resolver.url("scoverage-bintray", url("https://dl.bintray.com/sksamuel/sbt-plugins/"))(Resolver.ivyStylePatterns)
)

// Issues with scoverage 1.3.5 with scoveralls 1.0.3
// https://github.com/scoverage/sbt-coveralls/issues/73
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.0.1")

addSbtPlugin("org.scoverage" %% "sbt-coveralls" % "1.0.0")

addSbtPlugin("com.codacy" % "sbt-codacy-coverage" % "1.0.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.3")