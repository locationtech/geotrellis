package geotrellis.spark

import org.apache.spark.serializer.{KryoRegistrator => SparkKryoRegistrator}
import com.esotericsoftware.kryo.Kryo
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable

class KryoRegistrator extends SparkKryoRegistrator {
	override def registerClasses(kryo: Kryo) {
	  val r = kryo.register(classOf[TileIdWritable])
	  val s = kryo.register(classOf[ArgWritable])
	}
}