package geotrellis.spark.io

trait Bridge[A, B] extends (A => B) {
  override def apply(a: A): B
  def unapply(b: B): A
}