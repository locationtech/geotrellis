package geotrellis.spark.io

import spray.json.JsonFormat

trait LayerReindexer[ID, K, Container] {
  val attributeStore: AttributeStore[JsonFormat]
  val layerReader   : FilteringLayerReader[ID, K, Container]
  val layerDeleter  : LayerDeleter[ID]
  val layerMover    : LayerMover[ID]
  val layerCopier   : LayerCopier[ID]

  def tmpId(id: ID): ID

  def reindex(id: ID): Unit = {
    // TODO: define reindex strategy?
    val tmp = tmpId(id)

    layerCopier.copy(id, tmp) // TODO: to check at least it is unique?
    layerDeleter.delete(id)
    layerMover.move(tmp, id)
  }
}
