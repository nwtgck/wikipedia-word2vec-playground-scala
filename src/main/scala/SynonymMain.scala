import org.apache.spark.mllib.feature.Word2VecModel
import org.apache.spark.sql.SparkSession

import scala.util.Try

object SynonymMain {
  def main(args: Array[String]): Unit = {

    // The number of output synonyms
    val nSynonyms: Int = 5

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

    // Get word2vec model
    val word2VecModel: Word2VecModel = Word2VecModelGetter.getWord2VecModel(
      sparkSession = sparkSession,
      wikipediaPath = wikipediaPath,
      pageLimit     = pageLimit
    )

    // Print synonyms to stdout
    def printSynonyms(word: String): Unit = {
      println(s"==== Synonym of '${word}' ====")
      // `word` is in vocabulary
      if(word2VecModel.getVectors.isDefinedAt(word)){
        for(synonym <- word2VecModel.findSynonyms(word, nSynonyms)){
          println(s"synonym: ${synonym}")
        }
      } else {
        println(s"word '${word}' not found")
      }
    }

    // User input
    var inputWord: String = ""
    while({inputWord = scala.io.StdIn.readLine("word> "); inputWord != null}){
      // Find synonyms of user input
      printSynonyms(inputWord)
    }

  }

}
