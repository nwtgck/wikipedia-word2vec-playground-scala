import java.io.{BufferedOutputStream, File, FileOutputStream, ObjectOutputStream}

import info.bliki.wiki.filter.PlainTextConverter
import info.bliki.wiki.model.WikiModel
import org.apache.spark.sql.{Dataset, Encoders, SparkSession}
import io.github.nwtgck.wikipedia_dump_loader.{Page, Redirect, Revision, WikipediaDumpLoader}
import org.apache.spark.mllib.feature.{Word2Vec, Word2VecModel}
import org.apache.spark.rdd.RDD

object Main {
  def main(args: Array[String]): Unit = {
    require(args.length == 1)

    // Get Wikipedia Dump XML Path
    val wikipediaPath: String = args(0)

    // Create spark session
    val sparkSession: SparkSession = SparkSession
      .builder()
      .appName("Wikipedia Dump Loader Test [Spark session]")
      .master("local[*]")
      .config("spark.executor.memory", "1g")
      .getOrCreate()

    // Import implicits
    import sparkSession.implicits._

    // Parquet Path
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
        .map{page =>
          for{
            wikiText <- page.revision.textOpt
          } yield new WikiModel("", "").render(new PlainTextConverter, wikiText)
        }
        .filter(_.isDefined)
        .map(_.get)
        .limit(1000) // TODO: HARD CODE: This reduces the number of pages
        .cache()


    println(s"plainWikiDs.cout: ${plainWikiDs.count()}")

    // Create Words RDD
    val wordsRdd: RDD[Seq[String]] =
      plainWikiDs
        .rdd
        .map(text => text.split(" ").toSeq)
        .cache()

    // Create word2vec
    val word2vec: Word2Vec = new Word2Vec()

    // Learn
    val word2VecModel: Word2VecModel = word2vec.fit(wordsRdd)

    def printSynonyms(word: String): Unit = {
      println(s"==== Synonym of '${word}' ====")
      for(synonym <- word2VecModel.findSynonyms(word, 10)){
        println(s"synonym: ${synonym}")
      }
    }

    printSynonyms("the")
    printSynonyms("it")
    printSynonyms("of")
    printSynonyms("America")
    printSynonyms("Obama")



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
      // Write the Dataset to output
      pageDs
        .write
        .parquet(parquetPath)
      pageDs
    }
  }
}
