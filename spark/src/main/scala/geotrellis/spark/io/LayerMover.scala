package geotrellis.spark.io

trait LayerMover[ID] {
  def move(from: ID, to: ID): Unit
}
