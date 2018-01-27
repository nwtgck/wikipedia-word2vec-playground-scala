import java.io.File

import info.bliki.wiki.filter.PlainTextConverter
import info.bliki.wiki.model.WikiModel
import io.github.nwtgck.wikipedia_dump_loader.{Page, WikipediaDumpLoader}
import org.apache.spark.mllib.feature.{Word2Vec, Word2VecModel}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Dataset, Encoders, SparkSession}
import org.apache.spark.storage.StorageLevel

object Main {
  def main(args: Array[String]): Unit = {
    require(args.length == 1)

    // Get Wikipedia Dump XML Path
    val wikipediaPath: String = args(0)

    // Limit of pages
    // TODO: HARD CODE: This reduces the number of pages
    val pageLimit: Int = 1000

    // Create spark session
    val sparkSession: SparkSession = SparkSession
      .builder()
      .appName("Wikipedia Dump Loader Test [Spark session]")
      .master("local[*]")
      .config("spark.executor.memory", "1g")
      .getOrCreate()

    // Import implicits
    import sparkSession.implicits._

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

    // Create word2vec
    val word2vec: Word2Vec = new Word2Vec()

    // Learn
    val word2VecModel: Word2VecModel = word2vec.fit(wordsRdd)

    // Print synonyms to stdout
    def printSynonyms(word: String): Unit = {
      println(s"==== Synonym of '${word}' ====")
      for(synonym <- word2VecModel.findSynonyms(word, 10)){
        println(s"synonym: ${synonym}")
      }
    }

    printSynonyms("the")
    printSynonyms("it")
    printSynonyms("of")
    printSynonyms("america")
    printSynonyms("obama")



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
