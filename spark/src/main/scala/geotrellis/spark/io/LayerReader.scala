package geotrellis.spark.io

trait LayerReader[ID, ReturnType] extends Reader[ID, ReturnType] {
  val defaultNumPartitions: Int

  def read(id: ID, numPartitions: Int): ReturnType

  def read(id: ID): ReturnType =
    read(id, defaultNumPartitions)
}
