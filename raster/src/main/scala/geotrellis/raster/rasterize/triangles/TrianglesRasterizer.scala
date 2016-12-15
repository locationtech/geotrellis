package geotrellis.raster.rasterize.triangles

import geotrellis.raster._
import geotrellis.vector._

import com.vividsolutions.jts.geom.{ Envelope => JtsEnvelope }

import scala.collection.JavaConverters._


object TrianglesRasterizer {

  def apply(
    re: RasterExtent,
    sourceArray: Array[Double],
    triangles: Seq[Polygon],
    indexMap: Map[(Double, Double), Int]
  ): ArrayTile = {
    val tile = DoubleArrayTile.empty(re.cols, re.rows)
    apply(re, tile, sourceArray, triangles, indexMap)
    tile
  }

  def apply(
    re: RasterExtent,
    tile: MutableArrayTile,
    sourceArray: Array[Double],
    triangles: Seq[Polygon],
    indexMap: Map[(Double, Double), Int]
  ): Unit = {
    triangles
      .foreach({ triangle =>
        renderTriangle(triangle, re, sourceArray, tile, indexMap)
      })
  }

  def renderTriangle(
    triangle: Polygon,
    re: RasterExtent,
    sourceArray: Array[Double],
    tile: MutableArrayTile,
    indexMap: Map[(Double, Double), Int]
  ): Unit = {

    val Extent(xmin, ymin, xmax, ymax) = triangle.envelope
    val xn = ((xmin - re.extent.xmin) / re.cellwidth).toInt
    val yn = ((ymin - re.extent.ymin) / re.cellheight).toInt
    val xStart = re.extent.xmin + (xn + 0.5) * re.cellwidth
    val yStart = re.extent.ymin + (yn + 0.5) * re.cellheight
    val cols = math.ceil((xmax - xmin) / re.cellwidth).toInt
    val rows = math.ceil((ymax - ymin) / re.cellheight).toInt

    var row = 0; while (row < rows) {
      var col = 0; while (col < cols) {
        val x = xStart + col * re.cellwidth
        val y = yStart + row * re.cellheight
        val screenCol = ((x - re.extent.xmin) / re.cellwidth).toInt
        val screenRow = ((re.extent.ymax -y) / re.cellheight).toInt
        if (
          triangle.covers(Point(x,y)) &&
          screenCol < re.cols && screenRow < re.rows &&
          0 <= screenCol && 0 <= screenRow
        ) {
          val result = {
            val verts = triangle.vertices; require(verts.length == 4)
            val x1 = verts(0).x
            val y1 = verts(0).y
            val x2 = verts(1).x
            val y2 = verts(1).y
            val x3 = verts(2).x
            val y3 = verts(2).y
            val index1 = indexMap.getOrElse((verts(0).x, verts(0).y), throw new Exception)
            val index2 = indexMap.getOrElse((verts(1).x, verts(1).y), throw new Exception)
            val index3 = indexMap.getOrElse((verts(2).x, verts(2).y), throw new Exception)

            val determinant = (y2-y3)*(x1-x3)+(x3-x2)*(y1-y3)
            val lambda1 = ((y2-y3)*(x-x3)+(x3-x2)*(y-y3)) / determinant
            val lambda2 = ((y3-y1)*(x-x3)+(x1-x3)*(y-y3)) / determinant
            val lambda3 = 1.0 - lambda1 - lambda2

            lambda1*sourceArray(index1) + lambda2*sourceArray(index2) + lambda3*sourceArray(index3)
          }

          tile.setDouble(screenCol, screenRow, result)
        }
        col += 1
      }
      row += 1
    }
  }

}
