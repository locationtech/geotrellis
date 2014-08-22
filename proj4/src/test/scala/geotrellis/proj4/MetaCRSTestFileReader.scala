package geotrellis.proj4

import java.io.File

import org.parboiled2._
import scala.util.Try

/**
 * Reads a file in MetaCRS Test format
 * into a list of {@link MetaCRSTestCase}.
 * This format is a CSV file with a standard set of columns.
 * Each record defines a transformation from one coordinate system
 * to another.
 * For full details on the file format, see http://trac.osgeo.org/metacrs/
 * 
 * @author Martin Davis (port by Rob Emanuele)
 *
 */
object MetaCRSTestFileReader {
  final val COL_COUNT = 19

  private case class InternalCsvParser(input: ParserInput, delimeter: String) extends Parser {
    def DQUOTE = '"'
    def DELIMITER_TOKEN = rule(capture(delimeter))
    def DQUOTE2 = rule("\"\"" ~ push("\""))
    def CRLF = rule(capture("\n\r" | "\n"))
    def NON_CAPTURING_CRLF = rule("\n\r" | "\n")

    val delims = s"$delimeter\r\n"
    def TXT = rule(capture(!anyOf(delims) ~ ANY))
    val WHITESPACE = CharPredicate(" \t")
    def SPACES: Rule0 = rule(oneOrMore(WHITESPACE))

    def escaped = rule(optional(SPACES) ~
      DQUOTE ~ zeroOrMore(DELIMITER_TOKEN | TXT | CRLF | DQUOTE2) ~ DQUOTE ~
        optional(SPACES) ~> (_.mkString("")))
    def nonEscaped = rule(zeroOrMore(TXT) ~> (_.mkString("")))

    def field = rule(escaped | nonEscaped)
    def row: Rule1[Seq[String]] = rule(oneOrMore(field).separatedBy(delimeter))
    def file = rule(zeroOrMore(row).separatedBy(NON_CAPTURING_CRLF))

    def parsed() : Try[Seq[Seq[String]]] = file.run()
  }

  def parse(path: String): Seq[Seq[String]] = {
    val source = scala.io.Source.fromFile(path)
    try {
      val text = source.getLines filter(!_.startsWith("#")) drop(1) filter(_ != "") mkString "\n"
      new InternalCsvParser(ParserInput(text), ",").file.run().getOrElse(throw new RuntimeException)
    } finally {
      source.close
    }
  }
  
  def readTests(file: File): List[MetaCRSTestCase] = {
    parse(file.getAbsolutePath).map(parseTest).toList
  }

  private def parseTest(line: Seq[String]): MetaCRSTestCase = {
    // Weird CSV parser adding quotes to everything. Hack it out.
    val cols = line.map(s => s.stripPrefix("\"").stripSuffix("\"")).toArray

    if (cols.length != COL_COUNT)
      throw new IllegalStateException("Expected " + COL_COUNT+ " columns in file, but found " + cols.length)

    val testName    = cols(0)
    val testMethod  = cols(1)
    val srcCrsAuth  = cols(2)
    val srcCrs      = cols(3)
    val tgtCrsAuth  = cols(4)
    val tgtCrs      = cols(5)
    val srcOrd1     = parseNumber(cols(6))
    val srcOrd2     = parseNumber(cols(7))
    val srcOrd3     = parseNumber(cols(8))
    val tgtOrd1     = parseNumber(cols(9))
    val tgtOrd2     = parseNumber(cols(10))
    val tgtOrd3     = parseNumber(cols(11))
    val tolOrd1     = parseNumber(cols(12))
    val tolOrd2     = parseNumber(cols(13))
    val tolOrd3     = parseNumber(cols(14))
    val using       = cols(15)
    val dataSource  = cols(16)
    val dataCmnts   = cols(17)
    val maintenanceCmnts = cols(18)
    
    MetaCRSTestCase(testName,testMethod,srcCrsAuth,srcCrs,tgtCrsAuth,tgtCrs,srcOrd1,srcOrd2,srcOrd3,tgtOrd1,tgtOrd2,tgtOrd3,tolOrd1,tolOrd2,tolOrd3,using,dataSource,dataCmnts,maintenanceCmnts)
  }
 
  def parseNumber(numStr: String): Double = {
    if (numStr == null || numStr.length() == 0) {
      Double.NaN
    } else {
      numStr.toDouble
    }
  }
}
