import com.scalapenos.sbt.prompt.SbtPrompt.autoImport._
import org.scalastyle.sbt.ScalastylePlugin._
import sbt.Keys._
import sbt._
import scoverage.ScoverageSbtPlugin.autoImport._

import scala.util.Properties

val isSnapshot = true
val PlayVersion = "2.6.0-M2"
val actualVersion: String = s"0.16.${Properties.envOrElse("BUILD_NUMBER", "5-SNAPSHOT")}"

def withTests(project: Project) = project % "test->test;compile->compile"

val frontendCompilationSettings = Seq(
  organization := "de.welt",
  scalaVersion := "2.12.2",
  version in ThisBuild := s"${actualVersion}_${PlayVersion}${if (isSnapshot) "-SNAPSHOT" else ""}",

  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  publishArtifact in Test := false,
  promptTheme := com.scalapenos.sbt.prompt.PromptThemes.ScalapenosTheme
)

val frontendDependencyManagementSettings = Seq(
  resolvers := Seq(
    Resolver.mavenLocal,
    Resolver.jcenterRepo,
    Resolver.bintrayRepo("welt", "metrics-play")
  ),
  // https://www.typesafe.com/blog/improved-dependency-management-with-sbt-0137
  updateOptions := updateOptions.value.withCachedResolution(true)
)

val coreDependencySettings = Seq(
  libraryDependencies ++= Seq(
    "com.typesafe.play" %% "play" % PlayVersion % Provided,
    "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.0.0-RC1" % Test,
    "org.mockito" % "mockito-core" % "1.10.19" % Test,
    "com.typesafe.play" %% "play-cache" % PlayVersion % Provided,
    // Info:
    // Prevent compile warnings of: `Class javax.annotation.Nullable not found - continuing with a stub.`
    "com.google.code.findbugs" % "jsr305" % "3.0.1" % Compile,
    "com.google.inject" % "guice" % "4.1.0"
  )
)
val clientDependencySettings = Seq(
  libraryDependencies ++= Seq(
    "org.asynchttpclient" % "async-http-client" % "2.0.10",
    "ch.qos.logback" % "logback-classic" % "1.1.7",

    "com.amazonaws" % "aws-java-sdk-core" % "1.11.13",
    "com.amazonaws" % "aws-java-sdk-s3" % "1.11.13",

    "com.typesafe" % "config" % "1.3.0" % Provided,

    "com.typesafe.play" %% "play-ws" % PlayVersion % Provided,
    "com.typesafe.play" %% "play-cache" % PlayVersion % Provided,
    "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",

    "de.welt" %% "metrics-play" % "2.6.0-M2_32",

    "org.scalatestplus.play" %% "scalatestplus-play" % "3.0.0-RC1" % Test,
    "org.mockito" % "mockito-core" % "1.10.19" % Test
  )
)

val bintraySettings = Seq(
  pomExtra :=
    <scm>
      <url>git@github.com:WeltN24/
        {name.value}
        .git</url>
      <connection>scm:git:git@github.com:WeltN24/
        {name.value}
        .git</connection>
    </scm>
      <developers>
        <developer>
          <id>thisismana</id>
          <name>Matthias Naber</name>
          <url>https://github.com/thisismana</url>
        </developer>
        <developer>
          <id>harryurban</id>
          <name>Harry Urban</name>
          <url>https://github.com/harryurban</url>
        </developer>
        <developer>
          <id>bobaaaaa</id>
          <name>Patrick Dahms</name>
          <url>https://github.com/bobaaaaa</url>
        </developer>
      </developers>,
  bintrayRepository := "welt-content-api-client",
  bintrayOrganization := Some("welt"),
  bintrayVcsUrl := Some("git@github.com:you/your-repo.git")
)

def codeStyleSettings = Seq(
  // scoverage
  coverageExcludedPackages := "<empty>;",
  //    coverageMinimum := 25, // we are not that good yet ;)
  coverageFailOnMinimum := true,

  // scalastyle
  scalastyleConfig := file("scalastyle-config.xml"),
  scalastyleFailOnError := true
)

def project(id: String) = Project(id, base = file(id))
  .settings(frontendCompilationSettings: _*)
  .settings(frontendDependencyManagementSettings: _*)
  .settings(bintraySettings: _*)
  .settings(codeStyleSettings: _*)


// only in "welt-content-api-client"
val utils = project("utils")
  .settings(
    name := "welt-content-api-utils"
  )
  .settings(coreDependencySettings: _*)

val core = project("core")
  .settings(
    name := "welt-content-api-core"
  )
  .settings(coreDependencySettings: _*)

val raw = project("raw")
  .settings(clientDependencySettings: _*)
  .settings(
    name := "welt-content-api-raw"
  )
  .dependsOn(withTests(utils)).aggregate(utils)
  .dependsOn(withTests(core)).aggregate(core)

val pressed = project("pressed")
  .settings(
    name := "welt-content-api-pressed"
  )
  .settings(coreDependencySettings: _*)
  .dependsOn(withTests(core)).aggregate(core)

val coreClient = project("core-client")
  .settings(
    name := "welt-content-api-core-client"
  )
  .settings(clientDependencySettings: _*)
  .dependsOn(withTests(utils)).aggregate(utils)
  .dependsOn(withTests(core)).aggregate(core)

val rawClient = project("raw-client")
  .settings(
    name := "welt-content-api-raw-client"
  )
  .settings(clientDependencySettings: _*)
  .dependsOn(withTests(coreClient)).aggregate(coreClient)
  .dependsOn(withTests(raw)).aggregate(raw)

val pressedClient = project("pressed-client")
  .settings(
    name := "welt-content-api-pressed-client"
  )
  .settings(clientDependencySettings: _*)
  .dependsOn(withTests(coreClient)).aggregate(coreClient)
  .dependsOn(withTests(pressed)).aggregate(pressed)
  .dependsOn(withTests(rawClient)).aggregate(rawClient)

val main = Project("Root", base = file("."))
  .settings(
    name := "welt-content-api-root"
  )
  .settings(frontendCompilationSettings: _*)
  .settings(
    publish := {},
    bintrayUnpublish := {}
  )
  .aggregate(core, coreClient, raw, rawClient, pressed, pressedClient)
