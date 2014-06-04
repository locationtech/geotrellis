package geotrellis.feature.rasterize.polygon

import geotrellis._
import geotrellis.raster.RasterExtent
import geotrellis.feature._

case class TestLine(rowMin: Int, rowMax: Int, x0:Double, y0:Double, x1:Double, y1:Double, inverseSlope: Double) {
  def horizontal:Boolean = rowMin == rowMax
  
  def intercept(y:Double) =
    x0 + (y - y0) * inverseSlope
}

case class TestLineSet(testLines: Seq[TestLine], rowMin: Int, rowMax: Int) {
  def merge(other: TestLineSet): TestLineSet =
    TestLineSet(other.testLines ++ testLines, math.min(rowMin, other.rowMin), math.max(rowMax, other.rowMax))
}

object TestLineSet {
  lazy val EMPTY = TestLineSet(Seq(), Int.MaxValue, Int.MinValue)

  def apply(line: Line, re: RasterExtent): TestLineSet = {
    var rowMin = Int.MaxValue
    var rowMax = Int.MinValue

    val testLines = 
      line
        .points
        .sliding(2)
        .map { l =>
          val p1 = l(0)
          val p2 = l(1)

          val (x0, y0, x1, y1) =
            if (p1.y < p2.y) {
              (p1.x, p1.y, p2.x, p2.y)
            } else {
              (p2.x, p2.y, p1.x, p1.y)
            }
          
          val minRowDouble = re.mapYToGridDouble(y1)
          val maxRowDouble = re.mapYToGridDouble(y0)

          val minRow = (math.floor(re.mapYToGridDouble(y1) + 0.5)).toInt
          val maxRow = (math.floor(re.mapYToGridDouble(y0) - 0.5)).toInt

          val inverseSlope = (x1 - x0).toDouble / (y1 - y0).toDouble

          if (minRow > maxRow ||
              p1.y == p2.y ||
              inverseSlope == java.lang.Double.POSITIVE_INFINITY ||
              inverseSlope == java.lang.Double.NEGATIVE_INFINITY ) {
            // drop horizontal lines
            None
          } else {
            if(minRow < rowMin) rowMin = minRow
            if(maxRow > rowMax) rowMax = maxRow

            Some(TestLine(minRow, maxRow, x0, y0, x1, y1, inverseSlope))
          }
         }
        .flatten
        .toList
    
    TestLineSet(testLines, rowMin, rowMax)
  }
}
