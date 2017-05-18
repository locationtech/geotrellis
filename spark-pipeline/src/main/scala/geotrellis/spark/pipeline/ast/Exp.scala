package geotrellis.spark.pipeline.ast

trait Exp[T] {
  def lit(i: Int): T
  def neg(t: T): T
  def add(l: T, r: T): T
}
