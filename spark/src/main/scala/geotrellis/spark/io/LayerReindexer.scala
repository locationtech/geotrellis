package geotrellis.spark.io

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
