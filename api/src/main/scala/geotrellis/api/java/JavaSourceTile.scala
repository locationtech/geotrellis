package geotrellis.api.java

import java.{lang => jl}
import java.util.{Comparator, Iterator => JIterator, List => JList, Map => JMap}

import scala.collection.JavaConverters._
import scala.reflect.ClassTag

import geotrellis.proj4._

import geotrellis.raster._
import geotrellis.raster.merge._
import geotrellis.raster.reproject._

import geotrellis.spark._
import geotrellis.vector._

import org.apache.spark._
import org.apache.spark.rdd.RDD
import org.apache.spark.util.Utils

abstract class JavaSourceTile[K: Boundable: ClassTag, V <: Tile]
  extends JavaSourceTileLike[K, V]

trait JavaSourceTileLike[K, V] {
	def rdd: RDD[(K, V)]
}
