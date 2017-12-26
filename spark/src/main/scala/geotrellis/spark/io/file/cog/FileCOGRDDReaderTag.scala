package geotrellis.spark.io.file.cog

import geotrellis.raster.CellGrid
import geotrellis.spark._
import geotrellis.spark.io._

import com.typesafe.config.ConfigFactory
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json._

import scala.reflect.ClassTag

trait FileCOGRDDReaderTag[V <: CellGrid] extends FileCOGReader[V] {
  def read[K: SpatialComponent: Boundable: JsonFormat: ClassTag](
    keyPath: BigInt => String,
    baseQueryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(BigInt, BigInt)],
    readDefinitions: Map[SpatialKey, Seq[(SpatialKey, Int, TileBounds, Seq[(TileBounds, SpatialKey)])]],
    numPartitions: Option[Int] = None,
    threads: Int = ConfigFactory.load().getThreads("geotrellis.file.threads.rdd.read")
  )(implicit sc: SparkContext): RDD[(K, V)]
}
