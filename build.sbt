name := "wikipedia-word2vec-scala"

version := "0.1"

scalaVersion := "2.11.12"

val sparkVersion = "2.2.1"

// Add dependency of `wikipedia-dump-loader` in GitHub
// (from: https://github.com/sbt/sbt/issues/3489)
dependsOn(RootProject(uri("git://github.com/nwtgck/wikipedia-dump-loader-scala.git#6c63b83782f1e3249c001235d5e6057b98ecad5e")))

// https://mvnrepository.com/artifact/info.bliki.wiki/bliki-core
libraryDependencies += "info.bliki.wiki" % "bliki-core" % "3.1.0"
libraryDependencies += "org.apache.spark" %% "spark-mllib" % sparkVersion