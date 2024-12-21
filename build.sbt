import org.scalajs.linker.interface.ModuleInitializer
import org.scalajs.linker.interface.ModuleSplitStyle

lazy val lamicache = project
  .in(file("."))
  .enablePlugins(ScalaJSPlugin) // Enable the Scala.js plugin in this project
  .settings(
    scalaVersion                    := "3.6.2",
    scalacOptions ++= Seq(
      "-language:strictEquality",
      "-source:future",
      "-feature",
      "-deprecation",
      "-Xkind-projector:underscores",
      "-Xmax-inlines:256",
      "-Wall"
    ),
    scalaJSUseMainModuleInitializer := false,
    Compile / scalaJSModuleInitializers ++= Seq(
      ModuleInitializer.mainMethod("org.felher.lamicache.Main", "run").withModuleID("main"),
      ModuleInitializer.mainMethod("org.felher.lamicache.Worker", "sayIt").withModuleID("worker")
    ),
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("org.felher.lamicache")))
    },
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "2.8.0",
      "com.raquo"    %%% "laminar"     % "17.1.0",
      "org.felher"   %%% "beminar"     % "1.1.0"
    )
  )
