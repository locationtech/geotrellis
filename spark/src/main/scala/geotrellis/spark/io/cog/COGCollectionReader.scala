package geotrellis.spark.io.cog

import geotrellis.raster.CellGrid
import geotrellis.raster.merge.TileMergeMethods
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index.MergeQueue
import geotrellis.util._

import spray.json.JsonFormat
import java.net.URI

import scala.reflect.ClassTag

object COGCollectionReader {
  def read[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: TiffMethods: (? => TileMergeMethods[V])
  ](
    keyPath: BigInt => String, // keyPath
    pathExists: String => Boolean, // check the path above exists
    fullPath: String => URI, // add an fs prefix
    baseQueryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(BigInt, BigInt)],
    readDefinitions: Map[SpatialKey, Seq[(SpatialKey, Int, TileBounds, Seq[(TileBounds, SpatialKey)])]],
    threads: Int,
    numPartitions: Option[Int] = None
  )(implicit getByteReader: URI => ByteReader): Seq[(K, V)] = {
    if (baseQueryKeyBounds.isEmpty) return Seq.empty[(K, V)]

    val tiffMethods = implicitly[TiffMethods[V]]

    val ranges = if (baseQueryKeyBounds.length > 1)
      MergeQueue(baseQueryKeyBounds.flatMap(decomposeBounds))
    else
      baseQueryKeyBounds.flatMap(decomposeBounds)

    LayerReader.njoin[K, V](ranges.toIterator, threads) { index: BigInt =>
      println(keyPath(index))
      if (!pathExists(keyPath(index))) Vector()
      else {
        val uri = fullPath(keyPath(index))
        val baseKey = tiffMethods.getKey[K](uri)

        readDefinitions
          .get(baseKey.getComponent[SpatialKey])
          .toVector
          .flatten
          .flatMap { case (spatialKey, overviewIndex, _, seq) =>
            val key = baseKey.setComponent(spatialKey)
            val tiff = tiffMethods.readTiff(uri, overviewIndex)
            val map = seq.map { case (gb, sk) => gb -> key.setComponent(sk) }.toMap

            tiffMethods.tileTiff(tiff, map)
          }
      }
    }
    .groupBy(_._1)
    .map { case (key, (seq: Iterable[(K, V)])) => key -> seq.map(_._2).reduce(_ merge _) }
    .toSeq
  }
}
