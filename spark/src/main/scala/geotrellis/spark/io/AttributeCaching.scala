package geotrellis.spark.io

import geotrellis.spark.LayerId
import scala.util.Try

trait AttributeCaching { self: AttributeStore =>
  private val cache = new collection.mutable.HashMap[(LayerId, String), Any]

  def cacheRead[T: ReadableWritable](layerId: LayerId, attributeName: String): T = {
    cache.getOrElseUpdate(layerId -> attributeName, read[T](layerId, attributeName)).asInstanceOf[T]
  }

  def cacheWrite[T: ReadableWritable](layerId: LayerId, attributeName: String, value: T): Unit = {
    cache.update(layerId -> attributeName, value)
    write[T](layerId, attributeName, value)
  }

//  def layerExists(id: LayerId): Boolean = {
//    import spray.json._
//    import spray.json.DefaultJsonProtocol._
//    Try(cacheRead[JsValue](id, AttributeStore.Fields.keyIndex)).isSuccess
//  }

  def clearCache() = {
    cache.clear()
  }
}