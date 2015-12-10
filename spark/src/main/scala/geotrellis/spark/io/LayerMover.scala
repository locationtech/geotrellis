package geotrellis.spark.io

import spray.json.JsonFormat

class LayerMover[ID](attributeStore: AttributeStore[JsonFormat],
                              layerCopier   : LayerCopier[ID],
                              layerDeleter  : LayerDeleter[ID]) {
  def move(from: ID, to: ID): Unit = {
    layerCopier.copy(from, to)
    layerDeleter.delete(from)
  }
}
