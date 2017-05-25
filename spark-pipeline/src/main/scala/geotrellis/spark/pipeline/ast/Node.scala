package geotrellis.spark.pipeline.ast

trait Node[T] {
  def get: T
}

trait Read[T] extends Node[T]
trait Transform[F, T] extends Node[T]
trait Write[T] extends Node[T]
