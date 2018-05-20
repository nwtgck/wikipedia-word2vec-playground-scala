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

// (from: https://qiita.com/ytanak/items/97ecc67786ed7c5557bb)
assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".properties" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".xml" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".types" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".class" => MergeStrategy.first
  case "application.conf"                            => MergeStrategy.concat
  case "unwanted.txt"                                => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
