import java.io.File

import info.bliki.wiki.filter.PlainTextConverter
import info.bliki.wiki.model.WikiModel
import io.github.nwtgck.wikipedia_dump_loader.{Page, WikipediaDumpLoader}
import org.apache.spark.mllib.feature.{Word2Vec, Word2VecModel}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Dataset, Encoders, SparkSession}
import org.apache.spark.storage.StorageLevel

import scala.util.Try

object Main {
  def main(args: Array[String]): Unit = {

    // Get command line args
    // 0: Wikipedia Dump XML Path
    // 1: Limit of pages
    val (wikipediaPath: String, pageLimit: Int) = Try{
      val Array(wikipediaPath, pageLimitStr) = args
      (wikipediaPath, pageLimitStr.toInt)
    }.getOrElse({
      System.err.println("""Usage: sbt run "<dump xml path> <pageLimit>" """)
      sys.exit(1)
    })

    // Create spark session
    val sparkSession: SparkSession = SparkSession
      .builder()
      .appName("Wikipedia Dump Loader Test [Spark session]")
      .master("local[*]")
      .config("spark.executor.memory", "1g")
      .getOrCreate()

    // Import implicits
    import sparkSession.implicits._

    // Create word2vec
    val word2vec: Word2Vec = new Word2Vec()

    // Get word2vec model path
    val word2vecModelPath: String = s"out/pagelimit${pageLimit}.word2vec_model"

    // Get word2vec model
    val word2VecModel = if(new File(word2vecModelPath).exists()){
      // Load the existing model
      Word2VecModel.load(sparkSession.sparkContext, word2vecModelPath)
    } else {
      // Create a Parquet directory path
      val parquetPath: String = {
        val xmlFileName: String = new File(wikipediaPath).getName
        s"out/${xmlFileName}.ds.parquet"
      }

      // Get Page Dataset
      val pageDs: Dataset[Page] = loadPageDs(
        sparkSession,
        wikipediaPath = wikipediaPath,
        parquetPath   = parquetPath
      )

      println(s"Page length: ${pageDs.count()}")


      // Convert Dataset[Page] to plain text
      val plainWikiDs: Dataset[String] =
        pageDs
          .persist(StorageLevel.DISK_ONLY)
          .map{page =>
            for{
              wikiText <- page.revision.textOpt
            } yield new WikiModel("", "").render(new PlainTextConverter, wikiText)
          }
          .filter(_.isDefined)
          .map(_.get)
          .limit(pageLimit)


      println(s"plainWikiDs.count: ${plainWikiDs.count()}")

      // Create Words RDD
      val wordsRdd: RDD[Seq[String]] =
        plainWikiDs
          .rdd
          .persist(StorageLevel.DISK_ONLY)
          .map(text => text.toLowerCase.replaceAll("[^a-zA-Z0-9\\s]", "").split(" ").toSeq) // (from: https://github.com/snowplow/spark-example-project/blob/3729ce01f01ce2024694e7622fc5a30d58d967bf/src/main/scala/com/snowplowanalytics/spark/WordCount.scala#L47)

      // Learn
      val word2VecModel: Word2VecModel = word2vec.fit(wordsRdd)
      // Save the word2vec model
      word2VecModel.save(sparkSession.sparkContext, word2vecModelPath)
      word2VecModel
    }

    // Print synonyms to stdout
    def printSynonyms(word: String): Unit = {
      println(s"==== Synonym of '${word}' ====")
      // `word` is in vocabulary
      if(word2VecModel.getVectors.isDefinedAt(word)){
        for(synonym <- word2VecModel.findSynonyms(word, 10)){
          println(s"synonym: ${synonym}")
        }
      } else {
        println(s"word '${word}' not found")
      }
    }

    printSynonyms("the")
    printSynonyms("it")
    printSynonyms("of")
    printSynonyms("america")
    printSynonyms("obama")

    // User input
    var inputWord: String = ""
    while({inputWord = scala.io.StdIn.readLine("word> "); inputWord != null}){
      // Find synonyms of user input
      printSynonyms(inputWord)
    }



  }

  /**
    * Load Dataset[Page] by Wikipedia Dump XML path
    * @param sparkSession
    * @param wikipediaPath
    * @param parquetPath
    * @return
    */
  def loadPageDs(sparkSession: SparkSession, wikipediaPath: String, parquetPath: String): Dataset[Page] = {

    import sparkSession.implicits._

    // Get scheme of Page
    val pageScheme = Encoders.product[Page].schema

    if(new File(parquetPath).exists()){

      println(s"Load existing '${parquetPath}'")

      // Load page Dataset from Parquet
      val pageDs: Dataset[Page] =
        sparkSession
          .read
          .schema(pageScheme)
          .parquet(parquetPath)
          .as[Page]
      pageDs
    } else {
      // Load page Dataset from Wikipedia Dump XML
      val pageDs: Dataset[Page] = WikipediaDumpLoader.readXmlFilePath(
        sparkSession,
        filePath = wikipediaPath
      )
      // Write the Dataset to output directory
      pageDs
        .write
        .parquet(parquetPath)
      pageDs
    }
  }
}
