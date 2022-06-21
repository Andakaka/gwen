enablePlugins(GitVersioning)

// gwen core version
val gwenVersion = "3.16.2"

git.baseVersion := gwenVersion
git.useGitDescribe := true

lazy val gwen = (project in file("."))
  .settings(
    projectSettings,
    libraryDependencies ++= mainDependencies ++ testDependencies
  )

lazy val projectSettings = Seq(
  name := "gwen",
  description := "Automation and Robotics for Gherkin",
  scalaVersion := "3.1.2",
  organization := "org.gweninterpreter",
  homepage := Some(url("https://gweninterpreter.org")),
  organizationHomepage := Some(url("https://github.com/gwen-interpreter")),
  startYear := Some(2014),
  licenses += "Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"),
  versionScheme := Some("semver-spec"),
  trapExit := false,
  crossPaths := false,
  scalacOptions ++= Seq(
    "-feature",
    "-language:postfixOps",
    "-deprecation",
    "-Xtarget:8"
  ),
  initialize := {
    val _ = initialize.value
    val javaVersion = sys.props("java.specification.version")
    if (javaVersion != "1.8")
      sys.error(s"JDK 8 is required to build this project. Found $javaVersion instead")
  }
)

lazy val mainDependencies = {
  val cucumberGherkin = "23.0.1"
  val scalaLogging = "3.9.5"
  val slf4j = "1.7.32"
  val slf4jLog4j = "2.17.1"
  val scopt = "4.0.1"
  val jline = "2.14.6"
  val commonCodec = "1.15"
  val commonsText = "1.9"
  val scalaCSV = "1.3.10"
  val jsonPath = "2.7.0"
  val jodaTime = "2.10.14"
  val scalaTags = "0.11.1"
  val htmlCleaner = "2.26"
  val rpCommon = "5.3.3"
  val rpClientJava = "5.0.22"
  val tsConfig = "1.4.2"
  val jansi = "2.4.0"

  Seq(
    "io.cucumber" % "gherkin" % cucumberGherkin,
    "com.github.scopt" %% "scopt" % scopt,
    "com.typesafe.scala-logging" %% "scala-logging" % scalaLogging,
    "jline" % "jline" % jline,
    "org.apache.logging.log4j" % "log4j-slf4j-impl" % slf4jLog4j,
    "org.slf4j" % "jul-to-slf4j" % slf4j,
    "commons-codec" % "commons-codec" % commonCodec,
    "org.apache.commons" % "commons-text" % commonsText,
    "com.github.tototoshi" %% "scala-csv" % scalaCSV,
    "com.jayway.jsonpath" % "json-path" % jsonPath,
    "joda-time" % "joda-time" % jodaTime,
    "com.lihaoyi" %% "scalatags" % scalaTags,
    "net.sourceforge.htmlcleaner" % "htmlcleaner" % htmlCleaner,
    "com.typesafe" % "config" % tsConfig,
    "com.epam.reportportal" % "commons-model" % rpCommon,
    "com.epam.reportportal" % "client-java" % rpClientJava excludeAll(
      ExclusionRule(organization = "org.aspectj", name = "aspectjrt"),
      ExclusionRule(organization = "org.aspectj", name = "aspectjweaver")
    ),
    "org.fusesource.jansi" % "jansi" % jansi
  ) ++ mainOverrides
}

lazy val mainOverrides = {
  val jacksonDataBind = "2.13.3"
  val tikaCore = "1.28.3"
  val guava = "31.1-jre"

  Seq(
    "com.fasterxml.jackson.core" %  "jackson-databind" % jacksonDataBind,
    "org.apache.tika" % "tika-core" % tikaCore,
    "com.google.guava" % "guava" % guava
  )
}

lazy val testDependencies = {
  val scalaTest = "3.2.11"
  val scalaTestPlusMockito = "3.2.10.0"
  val mockitoCore = "3.12.4"
  val h2 = "1.4.200"
  // val slick = "3.3.3"

  Seq(
    "org.scalatest" %% "scalatest" % scalaTest,
    "org.scalatestplus" %% "mockito-3-4" % scalaTestPlusMockito,
    "org.mockito" % "mockito-core" % mockitoCore,
    "com.h2database" % "h2" % h2,
    // TODO: uncomment and re-enable SQLSupportTest once slick releases a Scala 3-compatible version
    // "com.typesafe.slick" %% "slick" % slick
  ).map(_ % Test)
}

Compile / packageBin / mappings ++= Seq(
  file("README.md") -> "README.txt",
  file("LICENSE") -> "LICENSE.txt",
  file("NOTICE") -> "NOTICE.txt",
  file("LICENSE-THIRDPARTY") -> "LICENSE-THIRDPARTY.txt",
  file("CHANGELOG") -> "CHANGELOG.txt"
)

Test/ parallelExecution := false
