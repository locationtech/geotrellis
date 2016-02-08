package geotrellis.spark

import geotrellis.spark.io.hadoop.{ KryoRegistrator => NormalKryoRegistrator }

import com.esotericsoftware.kryo.Kryo

import scala.util.Properties


class TestRegistrator extends NormalKryoRegistrator {
  override def registerClasses(kryo: Kryo) {
    super.registerClasses(kryo)
    if (Properties.envOrNone("GEOTRELLIS_KRYO_REGREQ") != None) {
      List(
        "geotrellis.proj4.CRS$$anon$1",
        "geotrellis.spark.io.avro.codecs.KeyCodecs$$anon$1",
        "geotrellis.spark.io.avro.codecs.KeyCodecs$$anon$2",
        "geotrellis.spark.io.avro.codecs.TileCodecs$$anon$1",
        "geotrellis.spark.io.avro.codecs.TileCodecs$$anon$2",
        "geotrellis.spark.io.avro.codecs.TileCodecs$$anon$3",
        "geotrellis.spark.io.avro.codecs.TileCodecs$$anon$4",
        "geotrellis.spark.io.avro.codecs.TileCodecs$$anon$5",
        "geotrellis.spark.io.avro.codecs.TileCodecs$$anon$6",
        "geotrellis.spark.io.avro.codecs.TileCodecs$$anon$7",
        "geotrellis.spark.io.avro.codecs.TileCodecs$$anon$8",
        "geotrellis.spark.SpaceTimeKey$$anonfun$ordering$1",
        "geotrellis.spark.SpatialKey$$anonfun$ordering$1",
        "geotrellis.spark.TemporalKey$$anonfun$ordering$1",
        "scala.math.Ordering$$anon$11",
        "scala.math.Ordering$$anon$9",
        "scala.math.Ordering$$anonfun$by$1",
        "scala.reflect.ClassTag$$anon$1"
      ).foreach({ anon => kryo.register(Class.forName(anon)) })

      try {
        // Needed for only one test
        kryo.register(Class.forName("geotrellis.spark.utils.OptimusPrime"))
      }
      catch {
        case e: java.lang.ClassNotFoundException =>
      }

      kryo.setRegistrationRequired(true)
    }
  }
}
