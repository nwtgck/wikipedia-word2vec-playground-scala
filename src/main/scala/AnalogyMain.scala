import breeze.linalg.DenseVector
import org.apache.spark.mllib.feature.Word2VecModel
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.sql.SparkSession

import scala.util.Try

object AnalogyMain {

  /**
    * Evaluate word expression
    * @param wordToVectorsMap
    * @param wordExpr
    * @return
    */
  def evalWordExpr(wordToVectorsMap: Map[String, DenseVector[Float]], wordExpr: WordExpr): Either[Exception, DenseVector[Float]] = {
    def _evalWordExpr(wordExpr: WordExpr): Either[Exception, DenseVector[Float]] = {
      wordExpr match {
        case RawWordExpr(word) => wordToVectorsMap.get(word) match {
          case None         => Left(new NoSuchElementException(s"word '${word}' not found"))
          case Some(vector) => Right(vector)
        }

        case AddWordExpr(lhs, rhs) => for{
          l <- _evalWordExpr(lhs).right
          r <- _evalWordExpr(rhs).right
        } yield l + r

        case SubWordExpr(lhs, rhs) => for{
          l <- _evalWordExpr(lhs).right
          r <- _evalWordExpr(rhs).right
        } yield l - r

      }
    }
    _evalWordExpr(wordExpr)
  }

  def main(args: Array[String]): Unit = {

    // The number of output synonyms
    // TODO: Hard code
    val nSynonyms: Int = 5

    // Get command line args
    // 0: Wikipedia Dump XML Path
    // 1: Limit of pages
    val (wikipediaPath: String, pageLimit: Int) = Try{
      val Array(wikipediaPath, pageLimitStr) = args
      (wikipediaPath, pageLimitStr.toInt)
    }.getOrElse({
      System.err.println("""Usage: sbt "runMain AnalogyMain <dump xml path> <pageLimit>" """)
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
      pageLimit     = pageLimit,
      word2VecNIterations = 100 // TODO: Hard code
    )

    // Generate word => vector map
    val wordToVectorsMap: Map[String, DenseVector[Float]] =
      word2VecModel.getVectors.map{case (word, vec) => (word, DenseVector(vec))}

    // Print analogies to stdout
    def printAnalogiesByWordExprStr(wordExprStr: String): Unit = {
      println(s"==== Analogy of '${wordExprStr}' ====")
      // Parse word expression
      WordExprParser(wordExprStr) match {
        case Right(wordExpr) =>
          evalWordExpr(wordToVectorsMap, wordExpr) match {
            case Left(exception) => println(s"Evaluation error: ${exception}")
            case Right(vector)   => {
              // Get synonyms
              val synonyms = word2VecModel.findSynonyms(Vectors.dense(vector.toArray.map(_.toDouble)), nSynonyms)
              for(synonym <- synonyms){
                println(s"analogy: ${synonym}")
              }
            }
          }
        case Left(noSuccess) =>
          println(s"Parse error: ${noSuccess}")
      }

    }

    // User input
    var wordExprStr: String = ""
    while({wordExprStr = scala.io.StdIn.readLine("> "); wordExprStr != null}){
      printAnalogiesByWordExprStr(wordExprStr)
    }

  }

}
