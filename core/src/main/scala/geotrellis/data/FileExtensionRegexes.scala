package geotrellis.data

import scala.util.matching.Regex


/**
  * Regular expressions to identify data format file extensions.
  */
object FileExtensionRegexes {
  val ArgPattern  =  new Regex(""".*\.arg$""")
  val GeoTiffPattern =  new Regex(""".*\.tif$""")
  val AsciiPattern  =  new Regex(""".*\.(asc|grd)$""")
}
