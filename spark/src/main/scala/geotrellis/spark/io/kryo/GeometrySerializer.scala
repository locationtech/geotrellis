package geotrellis.spark.io.kryo

import geotrellis.vector.Geometry
import geotrellis.vector.io.wkb.WKB

import com.esotericsoftware.kryo.{Kryo, Serializer}
import com.esotericsoftware.kryo.io.{Input, Output}
import com.typesafe.config.ConfigFactory

import java.io.ByteArrayOutputStream
import java.util.zip.{Deflater, Inflater}

class GeometrySerializer[G <: Geometry] extends Serializer[G] {
  override def write(kryo: Kryo, output: Output, geom: G): Unit = {
    val wkb = WKB.write(geom)

    val (data, compressed) =
      if (wkb.length > 1400) {
        // Only bother deflating data larger than about 1 packet worth
        val config = ConfigFactory.load
        val deflater = new Deflater(config.getInt("geotrellis.serialization.vector.compression.level"))
        deflater.setInput(wkb)
        deflater.finish
        val baos = new ByteArrayOutputStream
        val buffer = Array.ofDim[Byte](4096)
        try {
          while (!deflater.finished) {
            val size = deflater.deflate(buffer)
            baos.write(buffer, 0, size)
          }
        } finally {
          baos.close
        }

        (baos.toByteArray, true)
      } else {
        (wkb, false)
      }

    output.writeInt(data.length)
    output.writeBoolean(compressed)
    output.writeBytes(data)
  }

  override def read(kryo: Kryo, input: Input, t: Class[G]): G = {
    val bufLength = input.readInt
    val compressed = input.readBoolean
    val data = input.readBytes(bufLength)

    val wkb =
      if (compressed) {
        val inflater = new Inflater
        inflater.setInput(data)
        val baos = new ByteArrayOutputStream
        val buffer = Array.ofDim[Byte](4096)
        try {
          while (!inflater.finished) {
            val size = inflater.inflate(buffer)
            baos.write(buffer, 0, size)
          }
        } finally {
          baos.close
        }

        baos.toByteArray
      } else {
        data
      }

    WKB.read(wkb).asInstanceOf[G]
  }
}
