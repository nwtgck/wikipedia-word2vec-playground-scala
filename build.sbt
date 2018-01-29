name := "wikipedia-word2vec-playground"

version := "0.1"

scalaVersion := "2.11.12"

val sparkVersion  = "2.0.0"
val breezeVersion = "0.13.2"

libraryDependencies += "info.bliki.wiki" % "bliki-core" % "3.1.0"
libraryDependencies += "org.apache.spark" %% "spark-mllib" % sparkVersion % "provided"
libraryDependencies += "com.databricks" %% "spark-xml" % "0.4.1"

// Add dependency of `wikipedia-dump-loader` in GitHub
// (from: https://github.com/sbt/sbt/issues/3489)
dependsOn(RootProject(uri("git://github.com/nwtgck/wikipedia-dump-loader-scala.git#11fd4c0bd54d41bf7cf9813122ceeb3e8e757208")))
