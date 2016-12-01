package geotrellis.spark.pointcloud.dem

import io.pdal._ // Placed here to avoid "object pdal is not a member of package geotrellis.raster.io"

import geotrellis.raster._
import geotrellis.raster.rasterize.triangles.TrianglesRasterizer
import geotrellis.util.MethodExtensions
import geotrellis.vector._
import geotrellis.vector.voronoi.Delaunay


trait PointCloudDemMethods extends MethodExtensions[PointCloud] {

  /**
    * Compute the union of this PointCloud and the other one.
    */
  def union(other: Any): PointCloud = {
    val otherCloud = other match {
      case other: PointCloud => other
      case _ => throw new Exception
    }

    require(self.dimTypes == otherCloud.dimTypes)
    require(self.metadata == otherCloud.metadata)
    require(self.schema == otherCloud.schema)

    new PointCloud(self.bytes ++ otherCloud.bytes, self.dimTypes, self.metadata, self.schema)
  }

  lazy val xs = (0 until self.length).map({ i => self.getDouble(i, "X") }).toArray
  lazy val ys = (0 until self.length).map({ i => self.getDouble(i, "Y") }).toArray
  lazy val indexMap: Map[(Double, Double), Int] = xs.zip(ys).zipWithIndex.toMap
  lazy val triangles = Delaunay(xs, ys).triangles

  def toTile(re: RasterExtent, dimension: String): ArrayTile = {
    val sourceArray = (0 until self.length).map({ i => self.getDouble(i, dimension) }).toArray
    TrianglesRasterizer(re, sourceArray, triangles, indexMap)
  }

}
