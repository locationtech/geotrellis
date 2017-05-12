package geotrellis.spark.distance

import com.vividsolutions.jts.geom.Coordinate
import org.apache.spark.rdd.RDD

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.util.MethodExtensions
import geotrellis.vector.{Extent, MultiPoint, Point}

trait EuclideanDistanceRDDMethods extends MethodExtensions[RDD[(SpatialKey, Array[Coordinate])]] {
  def euclideanDistance(layout: LayoutDefinition): RDD[(SpatialKey, Tile)] = { EuclideanDistance(self, layout) }
}

trait EuclideanDistancePointRDDMethods extends MethodExtensions[RDD[(SpatialKey, Array[Point])]] {
  def euclideanDistance(layout: LayoutDefinition): RDD[(SpatialKey, Tile)] = { EuclideanDistance(self.mapValues(_.map(_.jtsGeom.getCoordinate)), layout) }
}

trait EuclideanDistancePairRDDMethods extends MethodExtensions[RDD[(SpatialKey, Array[(Double, Double)])]] {
  def euclideanDistance(layout: LayoutDefinition): RDD[(SpatialKey, Tile)] =
    EuclideanDistance(self.mapValues(_.map{ case (x, y) => new Coordinate(x, y) }), layout) 
}

trait EuclideanDistanceTripleRDDMethods extends MethodExtensions[RDD[(SpatialKey, Array[(Double, Double, Double)])]] {
  def euclideanDistance(layout: LayoutDefinition): RDD[(SpatialKey, Tile)] =
    EuclideanDistance(self.mapValues(_.map{ case (x, y, z) => new Coordinate(x, y, z) }), layout) 
}

trait EuclideanDistanceMultiPointRDDMethods extends MethodExtensions[RDD[(SpatialKey, MultiPoint)]] {
  def euclideanDistance(layout: LayoutDefinition): RDD[(SpatialKey, Tile)] =
    EuclideanDistance(self.mapValues(_.points.map(_.jtsGeom.getCoordinate)), layout) 
}
