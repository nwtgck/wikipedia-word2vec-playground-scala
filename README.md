## wikipedia-word2vec-playground

A playground of word2vec from [Wikipedia Dump](https://dumps.wikimedia.org/) with [Spark](https://spark.apache.org/)

## Run SynonymMain

```bash
cd <this repo>
sbt "runMain SynonymMain $HOME/Downloads/enwiktionary-20180101-pages-articles.xml 1000"
```

<img src="demo_images/word2vec_synonym.gif" width="600">

## Run AnalogyMain

```bash
cd <this repo>
sbt "runMain AnalogyMain $HOME/Downloads/enwiktionary-20180101-pages-articles.xml 1000"
```

<img src="demo_images/word2vec_analogy.gif" width="600">

## References

* <https://dumps.wikimedia.org/enwiktionary/20180101/>