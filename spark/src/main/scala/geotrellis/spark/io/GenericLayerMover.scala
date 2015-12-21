package geotrellis.spark.io

class GenericLayerMover[ID](layerCopier: LayerCopier[ID], layerDeleter: LayerDeleter[ID]) extends LayerMover[ID] {
  def move(from: ID, to: ID): Unit = {
    layerCopier.copy(from, to)
    layerDeleter.delete(from)
  }
}

object GenericLayerMover {
  def apply[ID](layerCopier: LayerCopier[ID], layerDeleter: LayerDeleter[ID]) =
    new GenericLayerMover(layerCopier, layerDeleter)
}
