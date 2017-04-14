package geotrellis.spark.distance

import com.vividsolutions.jts.geom.Coordinate
import org.apache.spark.rdd.RDD

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.util.MethodExtensions
import geotrellis.vector.{Extent, Point}

trait EuclideanDistanceRDDMethods extends MethodExtensions[RDD[(SpatialKey, Array[Coordinate])]] {
  def euclideanDistance(layout: LayoutDefinition): RDD[(SpatialKey, Tile)] = { EuclideanDistance(self, layout) }
}
