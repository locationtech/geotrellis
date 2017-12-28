package geotrellis.spark.io.cog

import geotrellis.raster.CellGrid
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.util.ByteReader
import geotrellis.spark.io.index.{IndexRanges, MergeQueue}
import geotrellis.spark.util._
import geotrellis.util._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat

import java.net.URI

import scala.reflect.ClassTag

trait COGRDDReader[V <: CellGrid] extends COGReader[V] {
  def read[K: SpatialComponent: Boundable: JsonFormat: ClassTag](
    keyPath: BigInt => String,
    pathExists: String => Boolean,
    baseQueryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(BigInt, BigInt)],
    readDefinitions: Map[SpatialKey, Seq[(SpatialKey, Int, TileBounds, Seq[(TileBounds, SpatialKey)])]],
    numPartitions: Option[Int] = None,
    threads: Int = defaultThreads
  )(implicit sc: SparkContext, getByteReader: URI => ByteReader, tiffMethods: TiffMethods[V]): RDD[(K, V)] = {
    if (baseQueryKeyBounds.isEmpty) return sc.emptyRDD[(K, V)]

    val kwFormat = KryoWrapper(implicitly[JsonFormat[K]])

    val ranges = if (baseQueryKeyBounds.length > 1)
      MergeQueue(baseQueryKeyBounds.flatMap(decomposeBounds))
    else
      baseQueryKeyBounds.flatMap(decomposeBounds)

    val bins = IndexRanges.bin(ranges, numPartitions.getOrElse(sc.defaultParallelism))

    sc.parallelize(bins, bins.size)
      .mapPartitions { partition: Iterator[Seq[(BigInt, BigInt)]] =>
        val keyFormat = kwFormat.value

        partition flatMap { seq =>
          LayerReader.njoin[K, V](seq.toIterator, threads) { index: BigInt =>
            println(s"${keyPath(index)}")
            if (!pathExists(keyPath(index))) Vector()
            else {
              val uri = fullPath(keyPath(index))
              val baseKey = tiffMethods.getKey[K](uri)(keyFormat)

              readDefinitions
                .get(baseKey.getComponent[SpatialKey])
                .flatMap(_.headOption)
                .map { case (spatialKey, overviewIndex, _, seq) =>
                  val key = baseKey.setComponent(spatialKey)
                  val tiff = tiffMethods.readTiff(uri, overviewIndex)
                  val map = seq.map { case (gb, sk) => gb -> key.setComponent(sk) }.toMap

                  tiffMethods.tileTiff(tiff, map)
                }
                .getOrElse(Vector())
            }
          }
        }
      }
      .groupBy(_._1)
      .map { case (key, (seq: Iterable[(K, V)])) => key -> seq.map(_._2).reduce(_ merge _) }
  }
}
