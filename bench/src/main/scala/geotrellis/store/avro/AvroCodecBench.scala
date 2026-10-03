package geotrellis.store.avro

import java.util.concurrent.TimeUnit

import geotrellis.layer.SpatialKey
import geotrellis.raster.*
import geotrellis.store.avro.codecs.*
import geotrellis.store.avro.codecs.Implicits.*

import org.apache.avro.Schema
import org.openjdk.jmh.annotations.*

/**
  * Mirrors the layer read/write path: one KeyValueRecordCodec per reader, writer schema parsed once.
  *
  * jmh:run -i 5 -wi 3 -f1 -t1 .*AvroCodecBench.*
  */
@BenchmarkMode(Array(Mode.AverageTime))
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
class AvroCodecBench {

  @Param(Array("16", "256"))
  var size: Int = _

  @Param(Array("1", "16"))
  var tilesPerRecord: Int = _

  var codec: KeyValueRecordCodec[SpatialKey, Tile] = _
  var writerSchema: Schema = _
  var record: Vector[(SpatialKey, Tile)] = _
  var bytes: Array[Byte] = _

  @Setup(Level.Trial)
  def setup(): Unit = {
    val rnd = new scala.util.Random(42)
    codec = KeyValueRecordCodec[SpatialKey, Tile]
    writerSchema = new Schema.Parser().parse(codec.schema.toString)
    record = (0 until tilesPerRecord).toVector.map { i =>
      SpatialKey(i, i) -> (FloatArrayTile(Array.fill(size * size)(rnd.nextFloat()), size, size): Tile)
    }
    bytes = AvroEncoder.toBinary(record)(codec)
  }

  @Benchmark
  def encode(): Array[Byte] = AvroEncoder.toBinary(record)(codec)

  @Benchmark
  def decode(): Vector[(SpatialKey, Tile)] = AvroEncoder.fromBinary(writerSchema, bytes)(codec)
}
