package geotrellis.spark.mapalgebra

import geotrellis.raster.Tile
import geotrellis.util.MethodExtensions
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag


trait TileCollectionMethods[K] extends MethodExtensions[Seq[(K, Tile)]]
