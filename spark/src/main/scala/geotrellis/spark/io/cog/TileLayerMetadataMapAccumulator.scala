package geotrellis.spark.io.cog

import geotrellis.spark.{Boundable, TileLayerMetadata}
import org.apache.spark.util.AccumulatorV2

import scala.collection.JavaConverters._
import java.util
import java.util.function.BiFunction

class TileLayerMetadataMapAccumulator[K: Boundable] extends AccumulatorV2[(Int, TileLayerMetadata[K]), util.Map[Int, TileLayerMetadata[K]]] {
  private val _map: util.Map[Int, TileLayerMetadata[K]] = util.Collections.synchronizedMap(new util.HashMap[Int, TileLayerMetadata[K]]())

  private def combineBiFunction =
    new BiFunction[TileLayerMetadata[K], TileLayerMetadata[K], TileLayerMetadata[K]] {
      def apply(fst: TileLayerMetadata[K], snd: TileLayerMetadata[K]): TileLayerMetadata[K] = fst combine snd
    }

  override def isZero: Boolean = _map.isEmpty

  override def copyAndReset(): TileLayerMetadataMapAccumulator[K] = new TileLayerMetadataMapAccumulator

  override def copy(): TileLayerMetadataMapAccumulator[K] = {
    val newAcc = new TileLayerMetadataMapAccumulator[K]
    _map.synchronized {
      newAcc._map.putAll(_map)
    }
    newAcc
  }

  override def reset(): Unit = _map.clear()

  override def add(v: (Int, TileLayerMetadata[K])): Unit =
    _map.merge(v._1, v._2, combineBiFunction)

  override def merge(other: AccumulatorV2[(Int, TileLayerMetadata[K]), util.Map[Int, TileLayerMetadata[K]]]): Unit =
    other match {
      case o: TileLayerMetadataMapAccumulator[K] =>
        o.value.asScala.foreach { case (k, v) => _map.merge(k, v, combineBiFunction) }
      case _ => throw new UnsupportedOperationException(
        s"Cannot merge ${this.getClass.getName} with ${other.getClass.getName}")
    }

  override def value: util.Map[Int, TileLayerMetadata[K]] = _map.synchronized {
    java.util.Collections.unmodifiableMap( new util.HashMap[Int, TileLayerMetadata[K]](_map))
  }

  private[spark] def setValue(newValue: util.Map[Int, TileLayerMetadata[K]]): Unit = {
    _map.clear()
    _map.putAll(newValue)
  }
}
