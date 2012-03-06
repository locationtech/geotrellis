package geotrellis.operation

import scala.collection.mutable.Map
import geotrellis.process._

/**
 * Create a Map of (String,String) => Int from a CSV file 
 * of the format: String,String,Int
 */
case class CsvIntMap(path:String,
                     delimiter:String) extends Op2(path, delimiter) ({
    (path, delimiter) => {
      val source = io.Source.fromFile(path)
      val lines  = source.getLines.toArray.filter {
        s => s.length > 0
      }.map {
        s => s.split(delimiter)
      }
  
      val width  = lines(0).length
      val height = lines.length
      val rows   = (1 until height).map { i => lines(i)(0) }
      val cols   = (1 until width).map  { j => lines(0)(j) }
    
      val map = Map.empty[(String, String), Int]
    
      var i = 1
      while (i < height) {
        val tokens = lines(i)
        if (tokens.length > width) {
          throw new Exception("row %d has too many columns".format(i))
        }
        var j = 1
        while (j < tokens.length) {
          val row = rows(i - 1)
          val col = cols(j - 1)
          val cell = lines(i)(j)
          map((row, col)) = cell.toInt
          j += 1
        }
        i += 1
      }
    
      Result(map)
  }
})
