package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.vector.Extent

class BilinearInterpolation(tile: Tile, extent: Extent) extends Interpolation {
  private val re = RasterExtent(tile, extent)
  private val cols = tile.cols
  private val rows = tile.rows

  // Define bounds outside of which we will consider the source as NoData
  private val westBound = extent.xmin
  private val eastBound = extent.xmax
  private val northBound = extent.ymax
  private val southBound = extent.ymin

  private val xmin = extent.xmin + (re.cellwidth / 2.0)
  private val xmax = extent.xmax - (re.cellwidth / 2.0)
  private val ymin = extent.ymin + (re.cellheight / 2.0)
  private val ymax = extent.ymax - (re.cellheight / 2.0)
  private val cellwidth = re.cellwidth
  private val cellheight = re.cellheight
  private val inverseDeltas = 1.0 / (cellwidth * cellheight)

  def interpolate(x: Double, y: Double): Int =
    if(x < westBound || eastBound < x ||
      y < southBound || northBound < y) {
      NODATA
    } else {
      var accum = 0.0
      var accumDivisor = 0.0

      val dleft = x - xmin   // distance to left raster border
      val dright = xmax - x  // distance to right raster border
      val dtop = ymax - y    // distance to upper raster border
      val dbottom = y - ymin // distance to lower raster border

      val leftCol = (dleft / cellwidth).toInt // closest left column
      val rightCol = leftCol + 1              // closest right column
      val topRow =  (dtop / cellheight).toInt // closest top row
      val bottomRow = topRow + 1              // closest bottom row

      val xRatio = if (dleft < 0) 1 else 1 - (dleft / cellwidth - leftCol)
      val yRatio = if (dtop < 0) 1 else 1 - (dtop / cellheight - topRow)

      // upper left pixel
      if (leftCol >= 0 && topRow >= 0 && leftCol < cols && topRow < rows) {
        val mult = xRatio * yRatio
        accumDivisor += mult
        accum += tile.get(leftCol, topRow) * mult
      }

      // upper right pixel
      if (rightCol >= 0 && topRow >= 0 && rightCol < cols && topRow < rows) {
        val mult = (1 - xRatio) * yRatio
        accumDivisor += mult
        accum += tile.get(rightCol, topRow) * mult
      }

      // lower left pixel
      if (leftCol >= 0 && bottomRow >= 0 && leftCol < cols && bottomRow < rows) {
        val mult = xRatio * (1 - yRatio)
        accumDivisor += mult
        accum += tile.get(leftCol, bottomRow) * mult
      }

      // lower right pixel
      if (rightCol >= 0 && bottomRow >= 0 && rightCol < cols && bottomRow < rows) {
        val mult = (1 - xRatio) * (1 - yRatio)
        accumDivisor += mult
        accum += tile.get(rightCol, bottomRow) * mult
      }

      // 1 -96.9523107579184
      // 5 -96.95044833710219
      // 6 -96.95007585293895
      // 17 -96.9459785271433
      // 63 -96.92884425563426
      // 79 -96.9228845090224
      // 264 -96.85397493882296
      if (x == -96.85397493882296 && y == 38.14379252760783) {
        println(s"accum: $accum, accumDivisor: $accumDivisor, xRatio: $xRatio, yRatio: $yRatio")
        println(s"result: ${(accum / accumDivisor).round.toInt}")
        println(s"topRow: $topRow, bottomRow: $bottomRow, leftCol: $leftCol, rightCol: $rightCol")
        println(s"($leftCol, $topRow): ${tile.get(leftCol, topRow)}")
        println(s"($rightCol, $topRow): ${tile.get(rightCol, topRow)}")
        println(s"($leftCol, $bottomRow): ${tile.get(leftCol, bottomRow)}")
        println(s"($rightCol, $bottomRow): ${tile.get(rightCol, bottomRow)}")
        println(s"(${leftCol + 1}, $topRow): ${tile.get(leftCol + 1, topRow)}")
        println(s"(${rightCol + 1}, $topRow): ${tile.get(rightCol + 1, topRow)}")
        println(s"(${leftCol - 1}, $topRow): ${tile.get(leftCol - 1, topRow)}")
        println(s"(${rightCol - 1}, $topRow): ${tile.get(rightCol - 1, topRow)}")
      }

      (accum / accumDivisor).round.toInt
    }

  def interpolateLeet(x: Double, y: Double): Int =
    if(x < westBound || eastBound < x ||
      y < southBound || northBound < y) {
      NODATA
    } else {
      // -8686.0 -8704.0
      // -7769.0 -7680.0
      // -30389.0 -15360.0

      val dleft = x - xmin
      val leftCol = (dleft / cellwidth).toInt
      val leftX = xmin + (leftCol * cellwidth)
      val dlx = x - leftX

      val dright = xmax - x
      val rightCol = leftCol + 1
      val rightX = leftX + cellwidth
      val drx = rightX - x

      val dbottom = ymax - y
      val bottomRow = (dbottom / cellheight).toInt
      val bottomY = ymax - (bottomRow * cellheight)
      val dby = bottomY - y

      val dtop = y - ymin
      val topRow = bottomRow + 1
      val topY = bottomY - cellheight
      val dty = y - topY

      val contribTopLeft =
        if (0 <= leftCol && leftCol < cols && 0 <= topRow && topRow < rows)
          tile.get(leftCol, topRow)
        else 0

      val contribTopRight =
        if (0 <= rightCol && rightCol < cols && 0 <= topRow && topRow < rows)
          tile.get(rightCol, topRow)
        else 0

      val contribBottomLeft =
        if (0 <= leftCol && leftCol < cols && 0 <= bottomRow && bottomRow < rows)
          tile.get(leftCol, bottomRow)
        else 0

      val contribBottomRight =
        if (0 <= rightCol && rightCol < cols && 0 <= bottomRow && bottomRow < rows)
          tile.get(rightCol, bottomRow)
        else 0

      val d1 = math.sqrt(dlx * dlx + dty * dty)
      val d2 = math.sqrt(drx * drx + dty * dty)
      val d3 = math.sqrt(dlx * dlx + dby * dby)
      val d4 = math.sqrt(drx * drx + dby * dby)

      val q = d1 + d2 + d3 + d4

      val f1 = contribTopLeft * (q - d1) / q
      val f2 = contribTopRight * (q - d2) / q
      val f3 = contribBottomLeft * (q - d3) / q
      val f4 = contribBottomRight * (q - d4) / q

      val res = f1 + f2 + f3 + f4

      // 1 -96.9523107579184
      // 5 -96.95044833710219
      // 17 -96.9459785271433

      if (x == -96.9523107579184 && y == 38.14379252760783) {
        println(s"cellwidth: $cellwidth, cellheight: $cellheight")
        println(s"${dleft / cellwidth}, ${dbottom / cellheight}")
        println(s"res: $res, topleft: $contribTopLeft, topright: $contribTopRight, bottomleft: $contribBottomLeft, bottomRight: $contribBottomRight")
        println(s"f1: $f1, f2: $f2, f3: $f3, f4: $f4")
        println(s"${(q  - d1) / q} ${(q - d2) / q} ${(q - d3) / q} ${(q - d4) / q}")
        println(s"leftCol: $leftCol, topRow: $topRow, rightCol: $rightCol, bottomRow: $bottomRow")
        println(s"dtop / cellheight: ${dtop / cellheight}, dbottom / cellheight: ${dbottom / cellheight}")
        println(s"ymax: $ymax, ymin: $ymin, dbottom: $dbottom")
      }

      res.toInt
    }

  def interpolateDouble(x: Double, y: Double): Double =
    if(x < westBound || eastBound < x ||
      y < southBound || northBound < y) {
      Double.NaN
    } else {
      val dleft = x - xmin
      val leftCol = (dleft / cellwidth).toInt
      val leftX = xmin + (leftCol * cellwidth)

      val dright = xmax - x
      val rightCol = leftCol + 1
      val rightX = leftX + cellwidth

      val dbottom = y - ymin
      val bottomRow = (dbottom / cellheight).toInt
      val bottomY = ymin + (bottomRow * cellheight)

      val dtop = ymax - y
      val topRow = bottomRow + 1
      val topY = bottomY + cellheight

      inverseDeltas *
      (tile.getDouble(leftCol, topRow) * dright * dbottom +
        tile.getDouble(rightCol, topRow) * dleft * dbottom +
        tile.getDouble(leftCol, bottomRow) * dright * dtop +
        tile.getDouble(rightCol, bottomRow) * dleft * dtop)
    }
}
