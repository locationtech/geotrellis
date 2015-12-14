package geotrellis.spark.io

import geotrellis.spark.LayerId

trait LayerReindexer[ID] {
  val layerDeleter  : LayerDeleter[ID]
  val layerMover    : LayerMover[ID]
  val layerCopier   : LayerCopier[ID]

  def getTmpId(id: ID): ID

  def reindex(id: ID): Unit = {
    // TODO: define reindex strategy?
    val tmpId = getTmpId(id)

    layerCopier.copy(id, tmpId) // TODO: to check at least it is unique?
    layerDeleter.delete(id)
    layerMover.move(tmpId, id)
  }
}

object LayerReindexer {
  def apply(lDeleter: LayerDeleter[LayerId],
            lCopier : LayerCopier[LayerId],
            lMover  : LayerMover[LayerId]): LayerReindexer[LayerId] =
    new LayerReindexer[LayerId] {
      val layerDeleter = lDeleter
      val layerCopier  = lCopier
      val layerMover   = lMover

      def getTmpId(id: LayerId): LayerId = id.copy(name = s"${id.name}-tmp")
    }
}
