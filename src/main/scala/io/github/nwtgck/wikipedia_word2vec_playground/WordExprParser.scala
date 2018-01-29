package io.github.nwtgck.wikipedia_word2vec_playground

import scala.util.parsing.combinator._

sealed trait WordExpr
case class RawWordExpr(word: String) extends WordExpr
case class AddWordExpr(lhs: WordExpr, rhs: WordExpr) extends WordExpr
case class SubWordExpr(lhs: WordExpr, rhs: WordExpr) extends WordExpr

// (from: http://www.scala-lang.org/api/2.12.3/scala-parser-combinators/scala/util/parsing/combinator/RegexParsers.html)
object WordExprParser extends RegexParsers {
  def rawWord: Parser[WordExpr] = """\w+""".r ^^ { w => RawWordExpr(w)}

  def factor: Parser[WordExpr] = rawWord | "(" ~> expr <~ ")"

  def expr: Parser[WordExpr] = factor ~ rep("+" ~ factor | "-" ~ factor) ^^ {
    case number ~ list => list.foldLeft(number) { // same as before, using alternate name for /:
      case (x, "+" ~ y) => AddWordExpr(x, y)
      case (x, "-" ~ y) => SubWordExpr(x, y)
    }
  }

  def apply(input: String): Either[NoSuccess, WordExpr] = parseAll(expr, input) match {
    case Success(result, _) => Right(result)
    case failure: NoSuccess => Left(failure)
  }
}