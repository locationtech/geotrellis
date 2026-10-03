package geotrellis.store.avro

import java.util.concurrent.TimeUnit

import geotrellis.layer.SpatialKey
import geotrellis.raster.*
import geotrellis.store.avro.codecs.*
import geotrellis.store.avro.codecs.Implicits.*

import org.apache.avro.Schema
import org.apache.avro.generic.{GenericDatumReader, GenericRecord}
import org.apache.avro.io.DecoderFactory
import org.openjdk.jmh.annotations.*

/**
  * Worst case for schema identity: a fresh codec and a freshly parsed writer schema on every call.
  * Forks run with a small heap, so a per-call leak fails the benchmark with an OOM; retained heap
  * (after GC) is also printed at the end of every iteration, so growth is visible before that.
  *
  * rawAvroControl is plain Avro 1.12 with the fast reader on and no caching: it is expected to OOM,
  * which shows the setup does detect a leak.
  *
  * jmh:run -i 5 -wi 2 .*AvroLeakBench.*
  */
@BenchmarkMode(Array(Mode.AverageTime))
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 1, jvmArgsAppend = Array("-Xmx256m"))
@Measurement(time = 10)
class AvroLeakBench {

  var schemaJson: String = _
  var bytes: Array[Byte] = _
  var rawBytes: Array[Byte] = _

  @Setup(Level.Trial)
  def setup(): Unit = {
    val codec = KeyValueRecordCodec[SpatialKey, Tile]
    schemaJson = codec.schema.toString
    val record = Vector(SpatialKey(0, 0) -> (FloatArrayTile(Array.fill(16 * 16)(1.5f), 16, 16): Tile))
    bytes = AvroEncoder.toBinary(record)(codec)
    rawBytes = AvroEncoder.decompress(bytes)
  }

  @TearDown(Level.Iteration)
  def retained(): Unit = {
    System.gc(); System.gc()
    val rt = Runtime.getRuntime
    println(s"\nretained after GC: ${(rt.totalMemory - rt.freeMemory) / (1024 * 1024)} MB")
  }

  @Benchmark
  def fromBinaryFreshSchemas(): Vector[(SpatialKey, Tile)] =
    AvroEncoder.fromBinary(new Schema.Parser().parse(schemaJson), bytes)(KeyValueRecordCodec[SpatialKey, Tile])

  @Benchmark
  def rawAvroControl(): GenericRecord =
    new GenericDatumReader[GenericRecord](new Schema.Parser().parse(schemaJson), KeyValueRecordCodec[SpatialKey, Tile].schema)
      .read(null, DecoderFactory.get().binaryDecoder(rawBytes, null))
}
