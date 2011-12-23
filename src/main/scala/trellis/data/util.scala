package trellis.data

import scala.util.matching.Regex


/**
  * Regular expressions to identify data format file extensions.
  */
object FileExtensionRegexes {
  val ArgPattern  =  new Regex(""".*\.arg$""")
  val Arg32Pattern  =  new Regex(""".*\.arg32$""")
  val GeoTiffPattern =  new Regex(""".*\.tif$""")
  val AsciiPattern  =  new Regex(""".*\.(asc|grd)$""")
}
