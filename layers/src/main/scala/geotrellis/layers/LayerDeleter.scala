package geotrellis.layers

trait LayerDeleter[ID] {
  def delete(id: ID): Unit
}
