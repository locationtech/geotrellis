package geotrellis.layers

import geotrellis.raster.CellGrid
import geotrellis.tiling._
import geotrellis.util._
import java.time.Instant


object Implicits extends Implicits

trait Implicits extends avro.codecs.Implicits
  with merge.Implicits
  with buffer.Implicits
  with json.Implicits
  with mapalgebra.Implicits
  with mapalgebra.focal.Implicits
  with mapalgebra.focal.hillshade.Implicits
  with mapalgebra.local.Implicits
  with mapalgebra.local.temporal.Implicits
  with mask.Implicits {

  implicit def longToInstant(millis: Long): Instant = Instant.ofEpochMilli(millis)

  /** Necessary for Contains.forPoint query */
  implicit def tileLayerMetadataToMapKeyTransform[K](tm: TileLayerMetadata[K]): MapKeyTransform = tm.mapTransform

  implicit class WithContextCollectionWrapper[K, V, M](val seq: Seq[(K, V)] with Metadata[M]) {
    def withContext[K2, V2](f: Seq[(K, V)] => Seq[(K2, V2)]) =
      new ContextCollection(f(seq), seq.metadata)

    def mapContext[M2](f: M => M2) =
      new ContextCollection(seq, f(seq.metadata))
  }

  implicit class withTileLayerCollectionMethods[K: SpatialComponent](val self: TileLayerCollection[K])
    extends TileLayerCollectionMethods[K]

  implicit class withCellGridLayoutCollectionMethods[K: SpatialComponent, V <: CellGrid[Int], M: GetComponent[?, LayoutDefinition]](val self: Seq[(K, V)] with Metadata[M])
    extends CellGridLayoutCollectionMethods[K, V, M]
}
