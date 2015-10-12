package geotrellis.spark.etl

import scala.reflect.runtime.universe._

trait TypedModule {
  var map: Map[TypeTag[_], () => _] = Map.empty

  def register[T: TypeTag](thing: => T): Unit =
    map = map updated (typeTag[T], () => thing)

  def find[T: TypeTag]: Option[T] =
    map
      .get(typeTag[T])
      .map(_.apply())
      .asInstanceOf[Option[T]]

  def findSubclassOf[T: TypeTag]: Seq[T] = {
    val target = typeTag[T]
    map
      .filterKeys( tt => tt.tpe <:< target.tpe)
      .values
      .map(_.apply())
      .toSeq
      .asInstanceOf[Seq[T]]
  }

  def union(other: TypedModule): TypedModule = {
    val tm = new TypedModule{}
    tm.map = map ++ other.map
    tm
  }
}
