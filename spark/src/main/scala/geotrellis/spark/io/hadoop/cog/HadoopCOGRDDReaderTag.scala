package geotrellis.spark.io.hadoop.cog

import geotrellis.raster.CellGrid
import geotrellis.spark._
import geotrellis.spark.io._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import com.typesafe.config.ConfigFactory
import spray.json.JsonFormat

import scala.reflect.ClassTag

trait HadoopCOGRDDReaderTag[V <: CellGrid] extends HadoopCOGReader[V] {
  def read[K: SpatialComponent: Boundable: JsonFormat: ClassTag](
    keyPath: BigInt => String,
    baseQueryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(BigInt, BigInt)],
    readDefinitions: Map[SpatialKey, Seq[(SpatialKey, Int, TileBounds, Seq[(TileBounds, SpatialKey)])]],
    numPartitions: Option[Int] = None,
    threads: Int = ConfigFactory.load().getThreads("geotrellis.hadoop.threads.rdd.read")
  )(implicit sc: SparkContext): RDD[(K, V)]
}
