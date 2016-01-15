package geotrellis.spark.io

import geotrellis.spark.LayerId
import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}
import org.joda.time.DateTime
import spray.json.JsonFormat

abstract class GenericLayerReindexer[ID, K](
  layerDeleter: LayerDeleter[ID],
  layerMover  : LayerMover[ID, K],
  layerCopier : LayerCopier[ID, K]) extends LayerReindexer[ID, K] {

  def getTmpId(id: ID): ID

  def reindex[I <: KeyIndex[K]: JsonFormat](id: ID, keyIndex: I): Unit = {
    val tmpId = getTmpId(id)
    layerCopier.copy(id, tmpId, keyIndex)
    layerDeleter.delete(id)
    layerMover.move(tmpId, id, keyIndex)
  }

  def reindex[I <: KeyIndex[K]: JsonFormat](id: ID, format: JsonFormat[I]): Unit = {
    val tmpId = getTmpId(id)
    layerCopier.copy(id, tmpId, format)
    layerDeleter.delete(id)
    layerMover.move(tmpId, id, format)
  }

  def reindex(id: ID, keyIndexMethod: KeyIndexMethod[K]): Unit = {
    val tmpId = getTmpId(id)
    layerCopier.copy(id, tmpId, keyIndexMethod)
    layerDeleter.delete(id)
    layerMover.move(tmpId, id, keyIndexMethod)
  }
}

object GenericLayerReindexer {
  def apply[K](layerDeleter: LayerDeleter[LayerId],
            layerCopier : LayerCopier[LayerId, K],
            layerMover  : LayerMover[LayerId, K]): LayerReindexer[LayerId, K] =
    new GenericLayerReindexer[LayerId, K](layerDeleter, layerMover, layerCopier) {
      def getTmpId(id: LayerId): LayerId = id.copy(name = s"${id.name}-${DateTime.now.getMillis}")
    }
}

