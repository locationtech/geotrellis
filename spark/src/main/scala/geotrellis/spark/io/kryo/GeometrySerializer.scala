package geotrellis.spark.io.kryo

import geotrellis.vector.Geometry
import geotrellis.vector.io.wkb.WKB

import com.esotericsoftware.kryo.{Kryo, Serializer}
import com.esotericsoftware.kryo.io.{Input, Output}

class GeometrySerializer[G <: Geometry] extends Serializer[G] {
  override def write(kryo: Kryo, output: Output, geom: G): Unit = {
    val wkb = WKB.write(geom)

    output.writeInt(wkb.length)
    output.writeBytes(wkb)
  }

  override def read(kryo: Kryo, input: Input, t: Class[G]): G = {
    val bufLength = input.readInt
    val wkb = input.readBytes(bufLength)

    WKB.read(wkb).asInstanceOf[G]
  }
}
