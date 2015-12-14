package geotrellis.spark.io

import spray.json.JsonFormat

trait LayerMover[ID] {
  val attributeStore: AttributeStore[JsonFormat]
  val layerCopier: LayerCopier[ID]
  val layerDeleter: LayerDeleter[ID]

  def move(from: ID, to: ID): Unit = {
    layerCopier.copy(from, to)
    layerDeleter.delete(from)
  }
}
