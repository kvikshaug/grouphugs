name := "grouphugs"

version := "1.0-SNAPSHOT"

scalaVersion := "2.9.1"

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked"
)

mainClass in (Compile, packageBin) := Some("no.kvikshaug.gh.Grouphug")

resolvers ++= Seq(
  "Kvikshaug" at "http://mvn.kvikshaug.no/"
)

libraryDependencies ++= Seq(
  "org.xerial" % "sqlite-jdbc" % "3.7.2",
  "kvikshaug" % "pircbot-patched" % "1.5.0",
  "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2",
  "jaxen" % "jaxen" % "1.1.1",
  "net.sourceforge.jchardet" % "jchardet" % "1.0",
  "joda-time" % "joda-time" % "1.6.2",
  "com.google.code.gson" % "gson" % "1.4",
  "kvikshaug" % "scatsd-client_2.9.1" % "1.0",
  "junit" % "junit" % "4.9" % "test",
  "com.novocode" % "junit-interface" % "0.7" % "test",
  "org.scalatest" % "scalatest_2.9.1" % "1.6.1" % "test"
)

parallelExecution in Test := false

// Write the classpath and copy the jar to "run/"
// Paths are hardcoded, feel free to make improvements

TaskKey[File]("mkrunnable") <<= (baseDirectory, fullClasspath in Runtime) map { (base, cp) =>
  import java.nio.file._
  val out = base / "run/classpath"
  IO.write(out, cp.files.absString)
  val fs = FileSystems.getDefault()
  val source = fs.getPath("target", "scala-2.9.1.final", "grouphugs_2.9.1-1.0-SNAPSHOT.jar")
  val dest = fs.getPath("run")
  Files.copy(source, dest.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING)
  out
}
