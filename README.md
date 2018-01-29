## wikipedia-word2vec-playground

A playground of word2vec from [Wikipedia Dump](https://dumps.wikimedia.org/) with [Spark](https://spark.apache.org/)

## Synonym

<img src="demo_images/word2vec_synonym.gif" width="600">

## Analogy

<img src="demo_images/word2vec_analogy.gif" width="600">

## Run Analogy in Cluster Mode in localhost

Here is an example to run Analogy in Cluster Mode in localhost

```bash
# Go to this repo
cd <this repo>
# Download & Extract spark commands (source: https://spark.apache.org/downloads.html)
curl https://archive.apache.org/dist/spark/spark-2.0.0/spark-2.0.0-bin-hadoop2.7.tgz | tar zxf -
# Run a master
./spark-2.0.0-bin-hadoop2.7/sbin/start-master.sh -h localhost -p 7077
# Run a slave
./spark-2.0.0-bin-hadoop2.7/sbin/start-slave.sh spark://localhost:7077
# Generate jar
sbt assembly
# Run Analogy
./spark-2.0.0-bin-hadoop2.7/bin/spark-submit --class "io.github.nwtgck.wikipedia_word2vec_playground.AnalogyMain" --master spark://localhost:7077 target/scala-2.11/wikipedia-word2vec-playground-assembly-0.1.jar $HOME/bigfiles/wikipedia_dumps/enwiktionary-20180101-pages-articles.xml 1000
```

## References

* <https://dumps.wikimedia.org/enwiktionary/20180101/>