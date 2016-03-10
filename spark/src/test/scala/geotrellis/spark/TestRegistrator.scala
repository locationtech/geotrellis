package geotrellis.spark

import geotrellis.spark.io.kryo.{ KryoRegistrator => NormalKryoRegistrator }

import com.esotericsoftware.kryo.Kryo

import scala.util.Properties


class TestRegistrator extends NormalKryoRegistrator {
  override def registerClasses(kryo: Kryo) {
    super.registerClasses(kryo)
    if (Properties.envOrNone("GEOTRELLIS_KRYO_REGREQ") != None) {

      kryo.register(classOf[geotrellis.proj4.CRS$$anon$1])
      kryo.register(classOf[geotrellis.spark.io.avro.codecs.KeyCodecs$$anon$1])
      kryo.register(classOf[geotrellis.spark.io.avro.codecs.KeyCodecs$$anon$2])
      kryo.register(classOf[geotrellis.spark.io.avro.codecs.TileCodecs$$anon$1])
      kryo.register(classOf[geotrellis.spark.io.avro.codecs.TileCodecs$$anon$2])
      kryo.register(classOf[geotrellis.spark.io.avro.codecs.TileCodecs$$anon$3])
      kryo.register(classOf[geotrellis.spark.io.avro.codecs.TileCodecs$$anon$4])
      kryo.register(classOf[geotrellis.spark.io.avro.codecs.TileCodecs$$anon$5])
      kryo.register(classOf[geotrellis.spark.io.avro.codecs.TileCodecs$$anon$6])
      kryo.register(classOf[geotrellis.spark.io.avro.codecs.TileCodecs$$anon$7])
      kryo.register(classOf[geotrellis.spark.io.avro.codecs.TileCodecs$$anon$8])
      kryo.register(classOf[geotrellis.spark.GridTimeKey$$anonfun$ordering$1])
      kryo.register(classOf[geotrellis.spark.GridKey$$anonfun$ordering$1])
      kryo.register(classOf[geotrellis.spark.TemporalKey$$anonfun$ordering$1])
      kryo.register(classOf[geotrellis.spark.util.OptimusPrime])
      kryo.register(classOf[scala.math.Ordering$$anon$11])
      kryo.register(classOf[scala.math.Ordering$$anon$9])
      kryo.register(classOf[scala.math.Ordering$$anonfun$by$1])
      kryo.register(classOf[scala.reflect.ClassTag$$anon$1])

      kryo.setRegistrationRequired(true)
    }
  }
}
