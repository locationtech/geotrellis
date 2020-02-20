package geotrellis.spark.io.cassandra.bench

import geotrellis.proj4.CRS
import geotrellis.raster._
import geotrellis.spark.io.cassandra.conf.{CassandraConfig, ReadOptimized}
import geotrellis.spark.io.cassandra.{BaseCassandraInstance, CassandraAttributeStore, CassandraInstance, CassandraLayerReader, CassandraLayerWriter, CassandraValueReader}
import geotrellis.spark.io.index.zcurve.ZSpatialKeyIndex
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.spark.{CassandraTestEnvironment, ContextRDD, KeyBounds, LayerId, SpatialKey, TileLayerMetadata}
import org.apache.spark.rdd.RDD
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index.ZCurveKeyIndexMethod
import geotrellis.util._
import geotrellis.spark.tiling._
import org.apache.spark.SparkContext._
import geotrellis.vector.{Extent, ProjectedExtent}
import org.scalatest._

import scala.concurrent.duration._

class CassandraIndexStrategySpec extends FunSpec with Matchers with CassandraTestEnvironment{

  val keyspace = "geotrellis_bench"

  val readAttrTable = "attributes_read"
  val readDataTable = "tiles_read"
  val writeAttrTable = "attributes_write"
  val writeDataTable = "tiles_write"

  val cassandraReadConfig = CassandraConfig(
    catalog = readAttrTable,
    keyspace = keyspace,
    indexStrategy = ReadOptimized,
    tilesPerPartition = 64

  )

  val cassandraWriteConfig = CassandraConfig(
    catalog = writeAttrTable,
    keyspace = keyspace
  )

  def instance(config: CassandraConfig) = BaseCassandraInstance(Seq("127.0.0.1"), config)

  def attributeStore(inst: CassandraInstance, attr: String): CassandraAttributeStore = CassandraAttributeStore(inst, keyspace, attr)
  def layerReader(attr: CassandraAttributeStore): CassandraLayerReader = CassandraLayerReader(attr)
  def layerWriter(attr: CassandraAttributeStore, key: String, data: String): CassandraLayerWriter = CassandraLayerWriter(attr, key, data)

  // Artificially construct a layer at Zoom Level 6:
  // 4096 = # of tiles in Zoom Level 6
  val keyBounds = KeyBounds(GridBounds(0, 0, 4095, 4095))
  val keyIndex = new ZSpatialKeyIndex(keyBounds)

  val layerId = LayerId("benchmark", 6)
  val tileLayout = TileLayout(64, 64, 256, 256) //zl 8 is 256 x 256 tiles, we'll default each tile to 256 x 256 cells.
  val worldExtent = ProjectedExtent(Extent(-180.0, -90.0, 180.0, 90.0), CRS.fromName("EPSG:4326"))
  val layoutDefinition = LayoutDefinition(worldExtent.extent, tileLayout)

  val layer = {
    val tiles = (0 until 64).flatMap{ i =>
      (0 until 64).map { j =>
        //Just store off the column we're in for the value in the whole tile...
        SpatialKey(i, j) -> BitArrayTile.fill(scala.util.Random.nextBoolean(), 256, 256)
      }
    }

    val rdd = sc.parallelize(tiles, Math.max((Runtime.getRuntime.availableProcessors() / 2) - 1, 1))

    ContextRDD[SpatialKey, BitArrayTile, TileLayerMetadata[SpatialKey]](rdd, TileLayerMetadata[SpatialKey](
      IntCellType, layoutDefinition, worldExtent.extent, worldExtent.crs, keyBounds
    ))
  }

  val inst_r = instance(cassandraReadConfig)
  val attr_r = attributeStore(inst_r, cassandraReadConfig.catalog)
  val reader_r = layerReader(attr_r)
  val writer_r = layerWriter(attr_r, keyspace, readDataTable)

  val inst_w = instance(cassandraWriteConfig)
  val attr_w = attributeStore(inst_w, cassandraReadConfig.catalog)
  val reader_w = layerReader(attr_w)
  val writer_w = layerWriter(attr_w, keyspace, writeDataTable)

  def timeRepeatedly[T](operation: String, numTrials: Int)(thunk: => T): List[(FiniteDuration, T)] = {
    (0 until numTrials).map{ i =>
      val start = System.currentTimeMillis()
      println(operation+" - Trial "+i)
      val t: T = thunk
      val stop = System.currentTimeMillis()
      (stop - start).millis -> t
    }.toList
  }

  it("writing a layer using the read-optimized schema should be within 1 sigma of the write-optimized variant"){

    writer_r.write(layerId, layer, keyIndex)
    val readResults = timeRepeatedly("Read-Optimized Schema Writes", 15){
      writer_r.overwrite(layerId, layer)
    }

    writer_w.write(layerId, layer, keyIndex)
    val writeResults = timeRepeatedly("Write-Optimized Schema Writes", 15){
      writer_w.overwrite(layerId, layer)
    }

    val avgRead = MathUtils.mean(readResults.map{_._1.toMillis})
    val avgWrite = MathUtils.mean(writeResults.map{_._1.toMillis})

    val stddevRead = MathUtils.stdDev(readResults.map{_._1.toMillis})
    val stddevWrite = MathUtils.stdDev(writeResults.map{_._1.toMillis})

    println("Average write-time for READ optimized schema: "+avgRead +"ms")
    println("Average write-time for WRITE optimized schema: "+avgWrite + "ms")

    println("STDDEV write-time for READ optimized schema: "+stddevRead + "ms")
    println("STDDEV write-time for WRITE optimized schema: "+stddevWrite + "ms")

    //val halfWrite = stddevWrite / 2.0
    val (upperWrite, lowerWrite) = (avgWrite + stddevWrite, avgWrite - stddevWrite)
    assert(avgRead >= lowerWrite && avgRead <= upperWrite,
      "Read-optimized writes were not within 1 sigma of write-optimized writes")

    //Sleep 10 seconds before continuing to give C* data a chance to stablize.
    println("Sleeping to allow Cassandra to synchronize writes...")
    Thread.sleep(10000)
    println("Done sleeping")
  }

  it("reading a layer using the read-optimized schema should be faster than the write-optimized variant"){

    import cats.effect._
    import cats.effect.syntax.all._
    import cats.implicits._
    import scala.concurrent.ExecutionContext
    import java.util.concurrent.Executors


    implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.fromExecutor(
      Executors.newCachedThreadPool()
    ))

    val valueReader_r = CassandraValueReader[SpatialKey, BitArrayTile](inst_r, attr_r, layerId)
    val valueReader_w = CassandraValueReader[SpatialKey, BitArrayTile](inst_w, attr_w, layerId)

    val spatialKeys = for{
      i <- 0 until 64
      j <- 0 until 64
    }yield{
      SpatialKey(i, j)
    }

    val grids = spatialKeys.sliding(16, 16).map{_.sliding(16, 16).map{IO(_)}}

    val ios = for{
      i <- grids
      j <- i
    }yield{
      j
    }

    val listOfIOs = ios.toList

    val readResults = timeRepeatedly("Read-Optimized Schema Reads", 100){

      val tiles = listOfIOs.map{ io => io.map{ keys =>
        keys.map{ key =>
          valueReader_r(key)
        }
      }}

      val allTiles = tiles.parSequence

      allTiles
        .attempt
        .unsafeRunSync()
        .valueOr(throw _)

      () //don't care about results... minimize heap churn
    }

    val writeResults = timeRepeatedly("Write-Optimized Schema Reads", 100){

      val tiles = listOfIOs.map{ io => io.map{ keys =>
        keys.map{ key => valueReader_w(key) }
      }}

      val allTiles = tiles.parSequence

      allTiles
        .attempt
        .unsafeRunSync()
        .valueOr(throw _)

      () //don't care about results... minimize heap churn
    }

    val avgRead = MathUtils.mean(readResults.map{_._1.toMillis})
    val avgWrite = MathUtils.mean(writeResults.map{_._1.toMillis})

    val stddevRead = MathUtils.stdDev(readResults.map{_._1.toMillis})
    val stddevWrite = MathUtils.stdDev(writeResults.map{_._1.toMillis})

    println("Average read-time for READ optimized schema: "+avgRead + "ms")
    println("Average read-time for WRITE optimized schema: "+avgWrite + "ms")

    println("STDDEV read-time for READ optimized schema: "+stddevRead + "ms")
    println("STDDEV read-time for WRITE optimized schema: "+stddevWrite + "ms")

    val oneTenthRead = stddevRead / 10.0
    val (upperRead, lowerRead) = (avgRead + oneTenthRead, avgRead - oneTenthRead)
    assert(avgWrite > upperRead,
      "Read-optimized reads were more than 0.1 sigma slower than write-optimized reads")
  }

}

object MathUtils{
  import Numeric.Implicits._

  def mean[T: Numeric](xs: Iterable[T]): Double = xs.sum.toDouble / xs.size.toDouble

  def variance[T: Numeric](xs: Iterable[T]): Double = {
    val avg = mean(xs)

    xs.map(_.toDouble).map(a => math.pow(a - avg, 2)).sum / xs.size.toDouble
  }

  def stdDev[T: Numeric](xs: Iterable[T]): Double = math.sqrt(variance(xs))
}
