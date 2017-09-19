import bintray.BintrayPlugin.autoImport._
import com.scalapenos.sbt.prompt.SbtPrompt.autoImport._
import org.scalastyle.sbt.ScalastylePlugin._
import sbt.Keys._
import sbt._
import scoverage.ScoverageSbtPlugin.autoImport._


object MyBuild extends Build {

  val isSnapshot = false
  val buildNumber = Option(System.getenv("BUILD_NUMBER")).getOrElse("local")
  val forScala2_4 = Option(System.getenv("PLAY24")).exists(_.toBoolean)

  val playVersion = if (forScala2_4) "2.4.8" else "2.5.10"
  private val actualVersion: String = s"0.18.$buildNumber"

  scalaVersion := "2.11.8"

  def withTests(project: Project) = project % "test->test;compile->compile"

  val frontendCompilationSettings = Seq(
    organization := "de.welt",
    scalaVersion := "2.11.8",
    version := s"$actualVersion${if (forScala2_4) "_2.4.0" else ""}${if (isSnapshot) "-SNAPSHOT" else ""}",

    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    publishArtifact in Test := false,
    promptTheme := com.scalapenos.sbt.prompt.PromptThemes.ScalapenosTheme
  )

  val frontendDependencyManagementSettings = Seq(
    resolvers := Seq(
      //      Resolver.typesafeRepo("releases"),
      Resolver.jcenterRepo
    ),
    // https://www.typesafe.com/blog/improved-dependency-management-with-sbt-0137
    updateOptions := updateOptions.value.withCachedResolution(true)
  )

  val coreDependencySettings = Seq(
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play" % playVersion % Provided,
      "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
      "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
      "org.mockito" % "mockito-core" % "1.10.19" % Test,
      "com.typesafe.play" %% "play-cache" % playVersion % Provided,
      // Info:
      // Prevent compile warnings of: `Class javax.annotation.Nullable not found - continuing with a stub.`
      "com.google.code.findbugs" % "jsr305" % "3.0.1" % Compile
    )
  )
  val clientDependencySettings = Seq(
    libraryDependencies ++= Seq(
      "org.asynchttpclient" % "async-http-client" % "2.0.10",
      "ch.qos.logback" % "logback-classic" % "1.1.7",

      "com.amazonaws" % "aws-java-sdk-core" % "1.11.13",
      "com.amazonaws" % "aws-java-sdk-s3" % "1.11.13",

      "com.typesafe" % "config" % "1.3.0" % Provided,

      "com.typesafe.play" %% "play-ws" % playVersion % Provided,
      "com.typesafe.play" %% "play-cache" % playVersion % Provided,
      "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",

      "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
      "org.mockito" % "mockito-core" % "1.10.19" % Test
    ) ++ (
      if (forScala2_4) {
        Seq("com.kenshoo" %% "metrics-play" % "2.4.0_0.4.1")
      }
      else {
        Seq("de.threedimensions" %% "metrics-play" % "2.5.13")
      })
  )


  val bintraySettings = Seq(
    pomExtra :=
      <scm>
        <url>git@github.com:WeltN24/{name.value}.git</url>
        <connection>scm:git:git@github.com:WeltN24/{name.value}.git</connection>
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

}
