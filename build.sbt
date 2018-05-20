name := "wikipedia-word2vec-playground"

version := "0.1"

scalaVersion := "2.11.12"

val sparkVersion  = "2.0.0"
val breezeVersion = "0.13.2"

libraryDependencies += "info.bliki.wiki" % "bliki-core" % "3.1.0"
libraryDependencies += "org.apache.spark" %% "spark-mllib" % sparkVersion % "provided"
libraryDependencies += "com.databricks" %% "spark-xml" % "0.4.1"
libraryDependencies += "com.github.scopt" %% "scopt" % "3.6.0"

// Add dependency of `spark-wikipedia-dump-loader` in GitHub
// (from: https://github.com/sbt/sbt/issues/3489)
dependsOn(RootProject(uri("git://github.com/nwtgck/spark-wikipedia-dump-loader.git#3ddea892f3650493d3af5bc8f2f8199b9e4c8548")))
