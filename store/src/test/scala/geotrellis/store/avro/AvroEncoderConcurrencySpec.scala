/*
 * Copyright 2026 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.store.avro

import geotrellis.layer.SpatialKey
import geotrellis.raster.*
import geotrellis.store.avro.codecs.*
import geotrellis.store.avro.codecs.Implicits.*

import org.apache.avro.{Schema, SchemaBuilder}
import org.apache.avro.generic.GenericRecord

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import java.util.concurrent.{Callable, CountDownLatch, Executors, TimeUnit}
import scala.jdk.CollectionConverters.*
import scala.util.Random

/**
  * Every thread writes and reads its own unique value, released at the same instant, so that corruption between
  * threads is detected.
  */
class AvroEncoderConcurrencySpec extends AnyFunSpec with Matchers {
  import AvroEncoderConcurrencySpec.*

  describe("AvroEncoder") {
    it("reads and writes unique tiles concurrently without corrupting data") {
      val threads = Threads
      val iterations = Iterations(ci = 20, stress = 100)
      val pool = Executors.newFixedThreadPool(threads)
      val codec = KeyValueRecordCodec[SpatialKey, Tile]

      try {
        for (i <- 0 until iterations) {
          // a fresh writer schema instance per iteration, as if read from layer metadata
          val writerSchema = new Schema.Parser().parse(codec.schema.toString)
          val expected = (0 until threads).map(t => Vector(SpatialKey(t, i) -> uniqueTile(i, t)))
          val bytes = expected.map(AvroEncoder.toBinary(_)(codec))

          val results = runTogether(pool, threads) { t =>
            (AvroEncoder.fromBinary(writerSchema, bytes(t))(codec), AvroEncoder.toBinary(expected(t))(codec))
          }

          results.zipWithIndex.foreach { case ((decoded, encoded), t) =>
            assertSameTiles(decoded, expected(t), s"iteration $i thread $t")
            withClue(s"iteration $i thread $t: re-encoded bytes differ from the original") {
              encoded.sameElements(bytes(t)) shouldBe true
            }
          }
        }
      } finally pool.shutdownNow()
    }

    it("reads and writes unique multiband tiles concurrently without corrupting data") {
      val threads = Threads
      val iterations = Iterations(ci = 10, stress = 50)
      val pool = Executors.newFixedThreadPool(threads)
      val codec = KeyValueRecordCodec[SpatialKey, MultibandTile]

      try {
        for (i <- 0 until iterations) {
          val writerSchema = new Schema.Parser().parse(codec.schema.toString)
          val expected = (0 until threads).map { t =>
            // bands of a multiband tile share one cell type; their values are still unique per band
            Vector(SpatialKey(t, i) -> (MultibandTile((0 until 3).map(b => uniqueTile(i, t, band = b))): MultibandTile))
          }
          val bytes = expected.map(AvroEncoder.toBinary(_)(codec))

          val results = runTogether(pool, threads) { t =>
            (AvroEncoder.fromBinary(writerSchema, bytes(t))(codec), AvroEncoder.toBinary(expected(t))(codec))
          }

          results.zipWithIndex.foreach { case ((decoded, encoded), t) =>
            val clue = s"iteration $i thread $t"
            withClue(clue) { decoded.map(_._1) shouldBe expected(t).map(_._1) }
            decoded.zip(expected(t)).foreach { case ((_, d), (_, e)) =>
              withClue(clue) { d.bandCount shouldBe e.bandCount }
              (0 until e.bandCount).foreach(b => assertSameTile(d.band(b), e.band(b), s"$clue band $b"))
            }
            withClue(s"$clue: re-encoded bytes differ from the original") { encoded.sameElements(bytes(t)) shouldBe true }
          }
        }
      } finally pool.shutdownNow()
    }

    it("reads and writes unique values concurrently through freshly cached readers and writers") {
      val threads = Threads
      val iterations = Iterations(ci = 100, stress = 300)
      val pool = Executors.newFixedThreadPool(threads)

      try {
        for (i <- 0 until iterations) {
          // a new schema per iteration, so every iteration starts from fresh cache entries
          val codec = recordCodec(i)
          val writerSchema = new Schema.Parser().parse(codec.schema.toString)
          val expected = (0 until threads).map(t => uniqueRec(i, t))
          val bytes = expected.map(AvroEncoder.toBinary(_)(codec))

          val results = runTogether(pool, threads) { t =>
            (AvroEncoder.fromBinary(writerSchema, bytes(t))(codec), AvroEncoder.toBinary(expected(t))(codec))
          }

          results.zipWithIndex.foreach { case ((decoded, encoded), t) =>
            withClue(s"iteration $i thread $t") { decoded shouldBe expected(t) }
            withClue(s"iteration $i thread $t: re-encoded bytes differ from the original") {
              encoded.sameElements(bytes(t)) shouldBe true
            }
          }
        }
      } finally pool.shutdownNow()
    }
  }

  /** Runs f(thread) on every thread, all released at the same instant. */
  private def runTogether[R](pool: java.util.concurrent.ExecutorService, threads: Int)(f: Int => R): IndexedSeq[R] = {
    val start = new CountDownLatch(1)
    val tasks = (0 until threads).map(t => pool.submit(new Callable[R] { def call(): R = { start.await(); f(t) } }))
    start.countDown()
    tasks.map(_.get(60, TimeUnit.SECONDS))
  }

  private def assertSameTiles(decoded: Vector[(SpatialKey, Tile)], expected: Vector[(SpatialKey, Tile)], clue: String): Unit = {
    withClue(clue) { decoded.map(_._1) shouldBe expected.map(_._1) }
    decoded.zip(expected).foreach { case ((_, d), (_, e)) => assertSameTile(d, e, clue) }
  }

  private def assertSameTile(decoded: Tile, expected: Tile, clue: String): Unit =
    withClue(clue) {
      decoded.cellType shouldBe expected.cellType
      (decoded.cols, decoded.rows) shouldBe ((expected.cols, expected.rows))
      decoded.toBytes().sameElements(expected.toBytes()) shouldBe true
    }
}

object AvroEncoderConcurrencySpec {
  /** -Dgeotrellis.avro.stress=true switches */
  val Stress: Boolean = sys.props.get("geotrellis.avro.stress").contains("true")
  val Threads: Int = if (Stress) 32 else 16
  def Iterations(ci: Int, stress: Int): Int = if (Stress) stress else ci

  val Fields = 40

  val CellTypes: Vector[CellType] = Vector(
    BitCellType,
    ByteConstantNoDataCellType, ByteCellType, ByteUserDefinedNoDataCellType(-7),
    UByteConstantNoDataCellType, UByteCellType,
    ShortConstantNoDataCellType, ShortCellType,
    UShortConstantNoDataCellType, UShortCellType,
    IntConstantNoDataCellType, IntCellType,
    FloatConstantNoDataCellType, FloatCellType,
    DoubleConstantNoDataCellType, DoubleCellType
  )

  /** A tile unique to (iteration, thread, band): cell type and size depend on (i, t), every cell value on all three. */
  def uniqueTile(i: Int, t: Int, band: Int = 0): Tile = {
    val cellType = CellTypes((i + t) % CellTypes.length)
    // size depends on the iteration only, so every cell type is covered at both sizes
    val size = if (i % 4 == 0) 256 else 64
    val rnd = new Random((i.toLong * 1000003L + t) * 31 + band)
    ArrayTile(Array.fill(size * size)(rnd.nextDouble() * 40000 - 20000), size, size).convert(cellType).toArrayTile()
  }

  final case class Rec(ints: Vector[Int], name: String, inner: Vector[Int])

  def uniqueRec(i: Int, t: Int): Rec =
    Rec((0 until Fields).map(f => i * 100000 + t * 1000 + f).toVector, s"name-$i-$t", (0 until 8).map(_ + i * 31 + t).toVector)

  def recordCodec(i: Int): AvroRecordCodec[Rec] = new AvroRecordCodec[Rec] {
    @transient lazy val schema: Schema = {
      val inner = SchemaBuilder.record(s"Inner$i").namespace("geotrellis.test")
        .fields()
        .name("name").`type`().stringType().noDefault()
        .name("values").`type`().array().items().intType().noDefault()
        .endRecord()

      val fields = (0 until Fields).foldLeft(SchemaBuilder.record(s"Rec$i").namespace("geotrellis.test").fields()) {
        (acc, f) => acc.name(s"f$f").`type`().intType().noDefault()
      }
      fields.name("inner").`type`(inner).noDefault().endRecord()
    }

    def encode(r: Rec, rec: GenericRecord): Unit = {
      r.ints.zipWithIndex.foreach { case (v, f) => rec.put(s"f$f", v) }
      val inner = new org.apache.avro.generic.GenericData.Record(schema.getField("inner").schema())
      inner.put("name", r.name)
      inner.put("values", r.inner.map(Int.box).asJava)
      rec.put("inner", inner)
    }

    def decode(rec: GenericRecord): Rec = {
      val inner = rec.get("inner").asInstanceOf[GenericRecord]
      Rec(
        (0 until Fields).map(f => rec.get(s"f$f").asInstanceOf[Int]).toVector,
        inner.get("name").toString,
        inner.get("values").asInstanceOf[java.util.Collection[Integer]].asScala.map(_.intValue).toVector
      )
    }
  }
}
