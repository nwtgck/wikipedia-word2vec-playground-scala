package io.github.nwtgck.wikipedia_word2vec_playground

import java.io.File

import info.bliki.wiki.filter.PlainTextConverter
import info.bliki.wiki.model.WikiModel
import io.github.nwtgck.wikipedia_dump_loader.{Page, WikipediaDumpLoader}
import org.apache.spark.mllib.feature.{Word2Vec, Word2VecModel}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Dataset, Encoders, SparkSession}
import org.apache.spark.storage.StorageLevel

object Word2VecModelGetter {

  def getWord2VecModel(sparkSession: SparkSession, wikipediaPath: String, pageLimit: Int, word2VecNIterations: Int, outDirPath: String): Word2VecModel = {
    // Import implicits
    import sparkSession.implicits._

    // Create word2vec
    val word2vec: Word2Vec = new Word2Vec()
      .setNumIterations(word2VecNIterations)

    // Get word2vec model path
    val word2vecModelPath: String = s"${outDirPath}/pagelimit${pageLimit}_iterations${word2VecNIterations}.word2vec_model"

    // Get word2vec model
    val word2VecModel = if(new File(word2vecModelPath).exists()){
      // Load the existing model
      Word2VecModel.load(sparkSession.sparkContext, word2vecModelPath)
    } else {
      // Create a Parquet directory path
      val parquetPath: String = {
        val xmlFileName: String = new File(wikipediaPath).getName
        s"${outDirPath}/${xmlFileName}.ds.parquet"
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

    word2VecModel
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
