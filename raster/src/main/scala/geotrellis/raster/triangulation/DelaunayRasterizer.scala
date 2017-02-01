package geotrellis.raster.triangulation

import com.vividsolutions.jts.geom.Coordinate

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.vector.triangulation._

object DelaunayRasterizer {
  // def time[T](msg: String)(f: => T) = {
  //   val start = System.currentTimeMillis
  //   val v = f
  //   val end = System.currentTimeMillis
  //   println(s"[TIMING-RAST] $msg: ${java.text.NumberFormat.getIntegerInstance.format(end - start)} ms")
  //   v
  // }


  // class IterationTimer(msg: String) {
  //   private val timings = scala.collection.mutable.ListBuffer[Int]()
  //   private def f(i: Int) = java.text.NumberFormat.getIntegerInstance.format(i)
  //   def time[T](f: => T) = {
  //     val start = System.currentTimeMillis
  //     val v = f
  //     val end = System.currentTimeMillis
  //     timings += ((end - start).toInt)
  //     v
  //   }

  //   def report() = {
  //     var sum = 0
  //     var count = 0
  //     var min = Int.MaxValue
  //     var max = -1
  //     var countAbove1ms = 0
  //     var countAbove1s = 0
  //     var numberOfMax = 0
  //     for(timing <- timings) {
  //       sum += timing
  //       count += 1
  //       if(timing < min) { min = timing }
  //       if(max < timing) { max = timing }
  //       if(timing > 1) { countAbove1ms += 1 }
  //       if(timing > 1000) { countAbove1s += 1 }
  //     }
  //     println(s"[TIMING ITERATION] $msg:")
  //     println(s"   Total: ${f(sum)} ms")
  //     println(s"   Count: ${f(count)}")
  //     println(s"    Mean: ${sum.toDouble / count} ms")
  //     println(s"     Min: ${f(min)} ms")
  //     println(s"     Max: ${f(max)} ms")
  //     println(s" # > 1ms: ${f(countAbove1ms)}")
  //     println(s"  # > 1s: ${f(countAbove1s)}")
  //   }
  // }


  /**
   * Produces a Tile with shape given by a RasterExtent where pixels are linearly interpolated
   * values of the z-coordinates of each triangle vertex.
   */
  def rasterize(
    tile: MutableArrayTile,
    re: RasterExtent,
    triangleMap: TriangleMap,
    halfEdgeTable: HalfEdgeTable,
    pointSet: DelaunayPointSet
  ): Unit = {
    val w = re.cellwidth
    val h = re.cellheight
    val cols = re.cols
    val rows = re.rows
    val Extent(exmin, eymin, exmax, eymax) = re.extent

    import halfEdgeTable._
    import pointSet._

    // val iTimer = new IterationTimer("Rasterization!")

    def rasterizeTriangle(tri: Int): Unit = /* iTimer.time */ {
      val e1 = tri
      val v1 = getDest(e1)
      val v1x = getX(v1)
      val v1y = getY(v1)
      val v1z = getZ(v1)
      val s1x = getX(getSrc(e1))
      val s1y = getY(getSrc(e1))

      val e2 = getNext(tri)
      val v2 = getDest(e2)
      val v2x = getX(v2)
      val v2y = getY(v2)
      val v2z = getZ(v2)
      val s2x = getX(getSrc(e2))
      val s2y = getY(getSrc(e2))

      val e3 = getNext(getNext(tri))
      val v3 = getDest(e3)
      val v3x = getX(v3)
      val v3y = getY(v3)
      val v3z = getZ(v3)
      val s3x = getX(getSrc(e3))
      val s3y = getY(getSrc(e3))

      val determinant =
        (v2y - v3y) * (v1x - v3x) + (v3x - v2x) * (v1y - v3y)

      val ymin =
        math.min(v1y, math.min(v2y, v3y))
      val ymax =
        math.max(v1y, math.max(v2y, v3y))

      val scanrow0 = math.max(math.ceil((ymin - eymin) / h - 0.5), 0)
      var scany = eymin + scanrow0 * h + h / 2
      while (scany < eymax && scany < ymax) {
        // get x at y for edge
        var xmin = Double.MinValue
        var xmax = Double.MaxValue

        if(s1y != v1y) {
          val t = (scany - v1y) / (s1y - v1y)
          val xAtY1 = v1x + t * (s1x - v1x)

          if(v1y < s1y) {
            // Lefty
            if(xmin < xAtY1) { xmin = xAtY1 }
          } else {
            // Rigty
            if(xAtY1 < xmax) { xmax = xAtY1 }
          }
        }

        if(s2y != v2y) {
          val t = (scany - v2y) / (s2y - v2y)
          val xAtY2 = v2x + t * (s2x - v2x)

          if(v2y < s2y) {
            // Lefty
            if(xmin < xAtY2) { xmin = xAtY2 }
          } else {
            // Rigty
            if(xAtY2 < xmax) { xmax = xAtY2 }
          }
        }

        if(s3y != v3y) {
          val t = (scany - v3y) / (s3y - v3y)
          val xAtY3 = v3x + t * (s3x - v3x)

          if(v3y < s3y) {
            // Lefty
            if(xmin < xAtY3) { xmin = xAtY3 }
          } else {
            // Rigty
            if(xAtY3 < xmax) { xmax = xAtY3 }
          }
        }

        val scancol0 = math.max(math.ceil((xmin - exmin) / w - 0.5), 0)
        var scanx = exmin + scancol0 * w + w / 2
        while (scanx < exmax && scanx < xmax) {
          val col = ((scanx - exmin) / w).toInt
          val row = ((eymax - scany) / h).toInt
          if(0 <= col && col < cols &&
             0 <= row && row < rows) {

            val z = {

              val lambda1 =
                ((v2y - v3y) * (scanx - v3x) + (v3x - v2x) * (scany - v3y)) / determinant

              val lambda2 =
                ((v3y - v1y) * (scanx - v3x) + (v1x - v3x) * (scany - v3y)) / determinant

              val lambda3 = 1.0 - lambda1 - lambda2

              lambda1 * v1z + lambda2 * v2z + lambda3 * v3z
            }

            tile.setDouble(col, row, z)
          }

          scanx += w
        }

        scany += h
      }
    }

    // time("RAST LOOP") {
      triangleMap.triangleEdges.foreach { e => rasterizeTriangle(e) }
    // }
    // time("Timer report:") { iTimer.report() }
  }

  def rasterizeDelaunayTriangulation(dt: DelaunayTriangulation, re: RasterExtent): Tile =
    rasterizeDelaunayTriangulation(dt, re, DoubleConstantNoDataCellType)

  def rasterizeDelaunayTriangulation(dt: DelaunayTriangulation, re: RasterExtent, cellType: CellType): Tile = {
    val tile: MutableArrayTile = ArrayTile.empty(cellType, re.cols, re.rows)
    rasterizeDelaunayTriangulation(dt, re, tile)
    tile
  }

  def rasterizeDelaunayTriangulation(dt: DelaunayTriangulation, re: RasterExtent, tile: MutableArrayTile): Unit =
    rasterize(tile, re, dt.triangles, dt.halfEdgeTable, dt.pointSet)
}
