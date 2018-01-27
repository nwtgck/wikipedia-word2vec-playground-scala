## wikipedia-word2vec-playground

A playground of word2vec from Wikipedia Dump with Spark

## Run SynonymMain

```bash
cd <this repo>
sbt "runMain SynonymMain $HOME/Downloads/enwiktionary-20180101-pages-articles.xml 1000"
```

![synonym](demo_images/word2vec_synonym.gif)

## Run AnalogyMain

```bash
cd <this repo>
sbt "runMain AnalogyMain $HOME/Downloads/enwiktionary-20180101-pages-articles.xml 1000"
```

![synonym](demo_images/word2vec_analogy.gif)

## References

* <https://dumps.wikimedia.org/enwiktionary/20180101/>