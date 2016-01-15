package geotrellis.spark.io

import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}
import spray.json.JsonFormat

class GenericLayerMover[ID, K](layerCopier: LayerCopier[ID, K], layerDeleter: LayerDeleter[ID]) extends LayerMover[ID, K] {
  def move[I <: KeyIndex[K]: JsonFormat](from: ID, to: ID, keyIndex: I): Unit = {
    layerCopier.copy(from, to, keyIndex)
    layerDeleter.delete(from)
  }

  def move[I <: KeyIndex[K]: JsonFormat](from: ID, to: ID, format: JsonFormat[I]): Unit = {
    layerCopier.copy(from, to, implicitly[JsonFormat[I]])
    layerDeleter.delete(from)
  }

  def move(from: ID, to: ID, keyIndexMethod: KeyIndexMethod[K]): Unit = {
    layerCopier.copy(from, to, keyIndexMethod)
    layerDeleter.delete(from)
  }
}

object GenericLayerMover {
  def apply[ID, K](layerCopier: LayerCopier[ID, K], layerDeleter: LayerDeleter[ID]) =
    new GenericLayerMover(layerCopier, layerDeleter)
}
