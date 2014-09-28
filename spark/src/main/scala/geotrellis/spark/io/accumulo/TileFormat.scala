package geotrellis.spark.io.accumulo

import org.apache.accumulo.core.data.{Key, Mutation, Value, Range => ARange}

/** Names a layer and a zoom level */
case class Layer(name: String, zoom: Int) {
  def asString = name
}