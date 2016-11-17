package geotrellis.raster.dem

import com.vividsolutions.jts.geom.{ Envelope => JtsEnvelope }
import com.vividsolutions.jts.index.strtree.STRtree
import geotrellis.raster._
import geotrellis.vector._
import geotrellis.vector.voronoi.Delaunay

import scala.collection.JavaConverters._


object PointCloud {
  def apply(points: Array[Point], zs: Array[Int]) = {
    val records = Map[LasRecordType, LasRecord](Z -> IntegralLasRecord(zs))
    val xs = points.map({ point => point.x })
    val ys = points.map({ point => point.y })

    new PointCloud(xs, ys, records)
  }
}

case class PointCloud(xs: Array[Double], ys: Array[Double], records: Map[LasRecordType, LasRecord]) {

  val size = xs.length

  require(ys.length == size)
  records.foreach({ pair =>
    pair._2 match {
      case record: CategoricalLasRecord => require(record.data.length == size)
      case record: FloatingLasRecord => require(record.data.length == size)
      case record: IntegralLasRecord => require(record.data.length == size)
    }
  })

  /**
    * Compute the union of this PointCloud and the other one.
    */
  def union(other: Any): PointCloud = {
    val otherCloud = other match {
      case other: PointCloud => other
      case _ => throw new Exception
    }
    val xs = (this.xs ++ otherCloud.xs)
    val ys = (this.ys ++ otherCloud.ys)
    val records = (this.records.toList ++ otherCloud.records.toList)
      .groupBy(_._1)
      .map({ pair =>
        val recordType = pair._1
        val recordList = pair._2.map(_._2)
        val record = recordType match {
          case _: CategoricalLasRecordType =>
            val data = recordList
              .map(_.asInstanceOf[CategoricalLasRecord].data)
              .reduce(_ ++ _)
            CategoricalLasRecord(data)
          case _: FloatingLasRecordType =>
            val data = recordList
              .map(_.asInstanceOf[FloatingLasRecord].data)
              .reduce(_ ++ _)
            FloatingLasRecord(data)
          case _: IntegralLasRecordType =>
            val data = recordList
              .map(_.asInstanceOf[IntegralLasRecord].data)
              .reduce(_ ++ _)
            IntegralLasRecord(data)
        }

        (recordType, record) })
      .toMap

    PointCloud(xs, ys, records)
  }

  val indexMap = xs.zip(ys).zipWithIndex.toMap

  /**
    * Compute a range tree over the Delaunay Triangulation of the
    * input points.  This is used for computing tiles when the input
    * point set is sparse in the extent of the output tile.
    */
  lazy val triangleTree = {
    val triangles = Delaunay(xs, ys).triangles
    val rtree = new STRtree

    triangles.foreach({ triangle =>
      val Extent(xmin, ymin, xmax, ymax) = triangle.envelope
      rtree.insert(new JtsEnvelope(xmin, xmax, ymin, ymax), triangle)
    })
    rtree
  }

  /**
    * Compute a tile by triangulating the source data and assigning
    * each pixel a value corresponding to the Barycentric
    * interpolation of the vertices of a Delaunay triangles that
    * contains it.  If more than one triangle contains a pixel, one of
    * the triangles is chosen arbitrarily (it should not matter since
    * the pixel should be the edge between the two triangles).  If no
    * triangle contains a pixel, then it is assigned NaN.
    *
    * @param  re  The raster extent which controls the rasterization.
    * @param  recordType  The record type (e.g. "z value") which should be used to produce the tile.
    */
  def toTile(re: RasterExtent, recordType: NumericalLasRecordType): ArrayTile = {
    val Extent(xmin, ymin, xmax, ymax) = re.extent
    val record = records.getOrElse(recordType, throw new Exception)
    val sourceArray = record match {
      case record: IntegralLasRecord => record.data.map(_.toDouble)
      case record: FloatingLasRecord => record.data
      case _ => throw new Exception
    }
    val targetArray = Array.ofDim[Double](re.cols * re.rows)

    /** Iterate over all columns and all rows ... */
    var i = 0
    var x = xmin + 0.5 * re.cellwidth; while (x < xmax) {
      var y = ymin + 0.5 * re.cellheight; while (y < ymax) {

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
        y += re.cellheight
      }
      x += re.cellwidth
    }

    DoubleArrayTile(targetArray, re.cols, re.rows)
  }

  /**
    * Compute a range tree over the input points.  This is used for
    * computing tiles when the input points are dense in the extent of
    * the output tile.
    */
  lazy val pointTree = {
    val rtree = new STRtree

    xs.zip(ys).foreach({ case (x, y) =>
      rtree.insert(new JtsEnvelope(x, x, y, y), Point(x, y))
    })
    rtree
  }

  /**
    * Compute a tile by averaging all of the values that occur in a
    * partcular pixel's area to compute that pixel's value.
    *
    * @note Use of this method is not recommended.
    */
  def toTileDenseData(re: RasterExtent, recordType: NumericalLasRecordType): ArrayTile = {
    val Extent(xmin, ymin, xmax, ymax) = re.extent
    val record = records.getOrElse(recordType, throw new Exception)
    val sourceArray = record match {
      case record: IntegralLasRecord => record.data.map(_.toDouble)
      case record: FloatingLasRecord => record.data
      case _ => throw new Exception
    }
    val targetArray = Array.ofDim[Double](re.cols * re.rows)

    /** Iterate over all columns and all rows ... */
    var i = 0
    var x = xmin; while (x < xmax) {
      var y = ymin; while (y < ymax) {

        /**
          * Get the collection of points covered by the extent of the
          * present pixel.  If there are any such points, average
          * their value.  If there are no such points, use NODATA.
          */
        val envelope = new JtsEnvelope(x, x + re.cellwidth, y, y + re.cellheight)
        val pointsInPixel = pointTree.query(envelope).asScala.map(_.asInstanceOf[Point])
        val result =
          if (pointsInPixel.length > 0) {
            val numerator = xs.zip(ys)
              .map({ point => sourceArray(indexMap.getOrElse(point, throw new Exception)) })
              .sum
            val denominator = pointsInPixel.length
            numerator / denominator
          } else {
            Double.NaN
          }

        targetArray(i) = result
        i += 1
        y += re.cellheight
      }
      x += re.cellwidth
    }

    DoubleArrayTile(targetArray, re.cols, re.rows)
  }

}
