package geotrellis.spark.io

import geotrellis.spark.LayerId
import org.joda.time.DateTime

abstract class GenericLayerReindexer[ID](
  layerDeleter: LayerDeleter[ID],
  layerMover  : LayerMover[ID],
  layerCopier : LayerCopier[ID]) extends LayerReindexer[ID] {

  def getTmpId(id: ID): ID

  def reindex(id: ID): Unit = {
    val tmpId = getTmpId(id)
    layerCopier.copy(id, tmpId)
    layerDeleter.delete(id)
    layerMover.move(tmpId, id)
  }
}

object GenericLayerReindexer {
  def apply(layerDeleter: LayerDeleter[LayerId],
            layerCopier : LayerCopier[LayerId],
            layerMover  : LayerMover[LayerId]): LayerReindexer[LayerId] =
    new GenericLayerReindexer[LayerId](layerDeleter, layerMover, layerCopier) {
      def getTmpId(id: LayerId): LayerId = id.copy(name = s"${id.name}-${DateTime.now.getMillis}")
    }
}

