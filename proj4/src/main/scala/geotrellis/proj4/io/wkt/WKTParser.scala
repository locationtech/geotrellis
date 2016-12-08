package geotrellis.proj4.io.wkt

import scala.io.Source
import geotrellis.proj4.Memoize
import org.osgeo.proj4j.NotFoundException

import scala.util.parsing.combinator.RegexParsers

object WKTParser extends RegexParsers {
// private lazy val wkttoepsgcodemap = new Memoize[String, String]
private val wktResourcePath = "/geotrellis/proj4/wkt/epsg.properties"

private def withWktFile[T](f: Iterator[String] => T) = {
        val stream = getClass.getResourceAsStream(wktResourcePath)
        try {
        f(Source.fromInputStream(stream).getLines())
        } finally {
        stream.close()
        }
        }

        def useMyFunction = createTree

        override def skipWhitespace: Boolean = false

private def createTree = {
        withWktFile { lines =>
        for (line <- lines) {
        val array = line.split("=")
        val code = array(0)

        def emptyString = ""
        def parseInnerStuff: Parser[String] = "[" ~> (emptyString | parseInnerStuff) <~ "]" ^^ {

        case x => {
        //print(x)
        emptyString
        }
        }
        def text = """[^\[\]]""".r
        def chunk = parseInnerStuff ^^ {_.toString} | text
        def chunks = rep1(chunk) ^^ {_.mkString} | ""
        if (array.length == 2) {
        // val parsed = text findAllIn array(1)
        //print(parsed)
        print(parseAll(chunk, array(1)))
        }

        }
        }
    /*def number = """\d+""".r ^^ { _.toInt }
    def sum: Parser[Int] = "{" ~> (number | sum) ~ "+" ~ (number | sum) <~ "}" ^^ {
      case x ~ "+" ~ y => x + y
    }
    def text = """[^{}]+""".r
    def chunk = sum ^^ {_.toString } | text
    def chunks = rep1(chunk) ^^ {_.mkString} | ""
    print(parseAll(chunks, "{2+10} lollies and {3+{4+5}} peanuts")) match {
      case Success(result, _) => result
      case failure: NoSuccess => scala.sys.error(failure.msg)
    }*/
        }



        }