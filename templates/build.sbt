name := "*project_name*"
version := "1.0.0"
scalaVersion := "3.1.1"

val akkaVersion = "2.6.20"
val akkaHttpVersion = "10.2.10"
val awsSdkVersion = "2.20.69"

enablePlugins(JavaAppPackaging, JavaAgent, DockerPlugin)

lazy val kamonVersion = "2.6.6"
libraryDependencies += "io.kamon" %% "kamon-core" % kamonVersion cross CrossVersion.for3Use2_13
libraryDependencies += "io.kamon" %% "kamon-bundle" % kamonVersion cross CrossVersion.for3Use2_13
libraryDependencies += "io.kamon" %% "kamon-akka-http" % kamonVersion cross CrossVersion.for3Use2_13
libraryDependencies += "io.kamon" %% "kamon-prometheus" % kamonVersion cross CrossVersion.for3Use2_13
libraryDependencies += "io.kamon" %% "kamon-opentelemetry" % kamonVersion cross CrossVersion.for3Use2_13

// Logging
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.4.7"
libraryDependencies += "ch.qos.logback.contrib" % "logback-jackson" % "0.1.5"
libraryDependencies += "ch.qos.logback.contrib" % "logback-json-classic" % "0.1.5"
libraryDependencies += "io.sentry" % "sentry-logback" % "6.19.0"

// AKKA
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaVersion cross CrossVersion.for3Use2_13
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" %  akkaVersion cross CrossVersion.for3Use2_13
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % akkaVersion cross CrossVersion.for3Use2_13
libraryDependencies += "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion cross CrossVersion.for3Use2_13

// AKKA http
libraryDependencies += "com.typesafe.akka" %% "akka-http" % akkaHttpVersion cross CrossVersion.for3Use2_13
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion cross CrossVersion.for3Use2_13
