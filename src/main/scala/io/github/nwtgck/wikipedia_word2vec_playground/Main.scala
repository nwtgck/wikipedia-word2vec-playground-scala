package io.github.nwtgck.wikipedia_word2vec_playground

import breeze.linalg.DenseVector
import org.apache.spark.mllib.feature.Word2VecModel
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.sql.SparkSession

sealed trait PlayMode
case object SynonymMode   extends PlayMode
case object AnalogyMode   extends PlayMode
case object TrainOnlyMode extends PlayMode


case class Word2VecPlaygroundOptions(wikipediaDumpPath: String = "",
                                     pageLimit: Int = -1,
                                     word2VecNIterations: Int = 100,
                                     playMode: PlayMode=SynonymMode,
                                     outDirPath: String="out")

object Main {

  def main(args: Array[String]): Unit = {

    // Parser for options
    val optParser = new scopt.OptionParser[Word2VecPlaygroundOptions]("Wikipedia Word2Vec Playground") {

      opt[String]("mode").required() action  {(v, options) =>
        v match {
          case "synonym" =>
            options.copy(playMode=SynonymMode)
          case "analogy" =>
            options.copy(playMode=AnalogyMode)
          case "train-only" =>
            options.copy(playMode=TrainOnlyMode)
        }
      } text ("play mode (e.g. 'synonym', 'analogy', 'train-only')")


      opt[String]("wikipedia-dump").required() action {(v, options) =>
        options.copy(wikipediaDumpPath=v)
      } text ("path of Wikipedia dump XML")

      opt[Int]("page-limit").required() action {(v, options) =>
        options.copy(pageLimit=v)
      } text ("limit of page to use")

      opt[Int]("word2vec-iterations") action {(v, options) =>
        options.copy(word2VecNIterations=v)
      } text ("the number of iterations of word2vec")

      opt[String]("out-dir") action {(v, options) =>
        options.copy(outDirPath=v)
      } text ("a path of output directory")
    }

    // Parse options
    val options: Word2VecPlaygroundOptions =
      optParser.parse(args, Word2VecPlaygroundOptions()).getOrElse({
        // NOTE: Usage will be outputed automatically
        sys.exit(1)
      })

    println(options)

    // The number of output synonyms
    // TODO: Hard code
    val nSynonyms: Int = 5

    val playMode           : PlayMode = options.playMode
    val wikipediaPath      : String   = options.wikipediaDumpPath
    val pageLimit          : Int      = options.pageLimit
    val word2VecNIterations: Int      = options.word2VecNIterations
    val outDirPath         : String   = options.outDirPath

    // Create spark session
    val sparkSession: SparkSession = SparkSession
      .builder()
      .appName("Wikipedia Word2Vec Playground")
      .config("spark.executor.memory", "1g")
      .getOrCreate()

    // Get word2vec model
    val word2VecModel: Word2VecModel = Word2VecModelGetter.getWord2VecModel(
      sparkSession        = sparkSession,
      wikipediaPath       = wikipediaPath,
      pageLimit           = pageLimit,
      word2VecNIterations = word2VecNIterations,
      outDirPath          = outDirPath
    )

    // Generate word => vector map
    val wordToVectorsMap: Map[String, DenseVector[Float]] =
      word2VecModel.getVectors.map{case (word, vec) => (word, DenseVector(vec))}

    // Print synonyms to stdout
    def printSynonyms(word: String): Unit = {
      println(s"==== Synonym of '${word}' ====")
      // `word` is in vocabulary
      if(wordToVectorsMap.isDefinedAt(word)){
        for(synonym <- word2VecModel.findSynonyms(word, nSynonyms)){
          println(s"synonym: ${synonym}")
        }
      } else {
        println(s"word '${word}' not found")
      }
    }

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


    playMode match {
      case TrainOnlyMode => () // DO NOTHING
      case SynonymMode =>
        // User input
        var inputWord: String = ""
        while({inputWord = scala.io.StdIn.readLine("word> "); inputWord != null}){
          // Find synonyms of user input
          printSynonyms(inputWord)
        }
      case AnalogyMode =>
        // User input
        var wordExprStr: String = ""
        while({wordExprStr = scala.io.StdIn.readLine("> "); wordExprStr != null}){
          printAnalogiesByWordExprStr(wordExprStr)
        }
    }
  }

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

}
