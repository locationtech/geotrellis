package geotrellis.raster.rasterize.triangles

import geotrellis.raster._
import geotrellis.vector._

import com.vividsolutions.jts.geom.{ Envelope => JtsEnvelope }
import com.vividsolutions.jts.index.strtree.STRtree

import scala.collection.JavaConverters._


object TrianglesRasterizer {

  def apply(
    re: RasterExtent,
    sourceArray: Array[Double],
    triangles: Array[Polygon],
    indexMap: Map[(Double, Double), Int]
  ): ArrayTile = {

    val triangleTree = {

      val rtree = new STRtree

      triangles.foreach({ triangle =>
        val Extent(xmin, ymin, xmax, ymax) = triangle.envelope
        rtree.insert(new JtsEnvelope(xmin, xmax, ymin, ymax), triangle)
      })
      rtree
    }
    apply(re, sourceArray, triangleTree, indexMap)
  }

  /**
    * Compute a tile by triangulating the source data and assigning
    * each pixel a value corresponding to the Barycentric
    * interpolation of the vertices of a Delaunay triangle(s) that
    * contains it.  If more than one triangle contains a pixel, one of
    * the triangles is chosen arbitrarily (it should not matter since
    * the pixel should be the edge between the two triangles).  If no
    * triangle contains a pixel, then it is assigned NaN.
    *
    * @param  re  The raster extent which controls the rasterization.
    * @param  recordType  The record type (e.g. "z value") which should be used to produce the tile.
    */
  def apply(
    re: RasterExtent,
    sourceArray: Array[Double],
    triangleTree: STRtree,
    indexMap: Map[(Double, Double), Int]
    ): ArrayTile = {

    val Extent(xmin, ymin, xmax, ymax) = re.extent
    val targetArray = Array.ofDim[Double](re.cols * re.rows)

    /** Iterate over all columns and all rows ... */
    var i = 0
    var y = ymin + 0.5 * re.cellheight; while (y < ymax) {
      var x = xmin + 0.5 * re.cellwidth; while (x < xmax) {

        /**
          * Find the triangle in which the point lies.  Use
          * Barycentric Interpolation [1] to compute the value at that
          * point.
          *
          * 1. https://en.wikipedia.org/wiki/Barycentric_coordinate_system#Interpolation_on_a_triangular_unstructured_grid
          */
        val envelope = new JtsEnvelope(x, x, y, y)
        val triangles = triangleTree.query(envelope).asScala.map(_.asInstanceOf[Polygon]).filter(_.covers(Point(x,y)))
        val result =
          if (triangles.length > 0) {
            val triangle = triangles.head
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
          } else {
            Double.NaN
          }

        targetArray(i) = result
        i += 1
        x += re.cellwidth
      }
      y += re.cellheight
    }

    DoubleArrayTile(targetArray, re.cols, re.rows)
  }

}
