package geotrellis.spark.io.accumulo

import geotrellis.raster.{Tile, CellType, ArrayTile}
import geotrellis.spark._
import geotrellis.spark.ingest.IngestNetCDF.TimeBandTile
import geotrellis.spark.rdd.{LayerMetaData, RasterRDD}
import geotrellis.spark.tiling.TileCoordScheme
import org.apache.accumulo.core.client.mapreduce.{AccumuloOutputFormat, InputFormatBase, AccumuloInputFormat}
import org.apache.accumulo.core.data.{Key, Mutation, Value, Range => ARange}
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.rdd.RDD
import org.apache.accumulo.core.util.{Pair => JPair}
import scala.collection.JavaConversions._

/** This trait represents a query that could be understood by a class implementing AccumuloFormat[K] */
trait RasterQuery[K]

case class SpaceQuery(tileBounds: Option[(TileBounds, TileCoordScheme)]) extends RasterQuery[TileId]
case class SpaceTimeQuery(tileBounds: Option[(TileBounds, TileCoordScheme)],
                          timeBounds: Option[(Double, Double)]) extends RasterQuery[TimeBandTile]

trait AccumuloFormat[K, Q <: RasterQuery[K]] {
  /** Map rdd of indexed tiles to tuples of (table name, row mutation) */
  def encode(raster: RasterRDD[K], layer: String): RDD[(Text, Mutation)]

  /** Delegate for setting query ranges and iterators */
  def setQueryParams(job: Job, metaData: LayerMetaData, query: Q): Unit

  /** Maps RDD of Accumulo specific Key, Value pairs to a tuple of (K, Tile) and wraps it in RasterRDD */
  def decode(rdd: RDD[(Key, Value)], metaData: LayerMetaData): RasterRDD[K]
}

/** Format to be used when Raster RDD is indexed only by TileId */
object FlatAccumuloFormat extends AccumuloFormat[TileId, SpaceQuery] {
  val rowIdRx = """(\d+)_(\d+)""".r // (zoom)_(TmsTilingId)
  def rowId(id: TileId, md: LayerMetaData) = new Text(s"${md.level.id}_${id}")

  def encode(raster: RasterRDD[TileId], layer: String): RDD[(Text, Mutation)] =
    raster.map{ case (id, tile) =>
      val mutation = new Mutation(rowId(id, raster.metaData))
      mutation.put(
        new Text(layer), null,
        System.currentTimeMillis(),
        new Value(tile.toBytes()))
      (null, mutation)
    }

  def setQueryParams(job: Job, metaData: LayerMetaData, query: SpaceQuery): Unit = {
    import geotrellis.spark.tiling._
    import scala.collection.JavaConversions._

    val ranges: Seq[ARange] =
    query.tileBounds match {
      case None =>
        new ARange(
          new Text(s"${metaData.level.id}_0"),
          new Text(s"${metaData.level.id}_9")
        ) :: Nil
      case Some((tileBounds, coordScheme)) =>
        metaData.transform
          .withCoordScheme(coordScheme)
          .tileToIndex(tileBounds)
          .spans
          .map { ts =>
          new ARange(rowId(ts._1, metaData), rowId(ts._2, metaData))
        }
    }

    InputFormatBase.setRanges(job, ranges)
  }

  def decode(rdd: RDD[(Key, Value)], metaData: LayerMetaData): RasterRDD[TileId] = {
    val tileRdd = rdd.map {
      case (key, value) =>
        val rowIdRx(_, id) = key.getRow.toString
        val tile = ArrayTile.fromBytes(value.get,
          metaData.cellType, metaData.tileLayout.tileCols, metaData.tileLayout.tileRows)
        id.toLong -> tile.asInstanceOf[Tile]
    }
    new RasterRDD(tileRdd, metaData)
  }
}

object TimeBandAccumuloFormat extends AccumuloFormat[TimeBandTile, SpaceTimeQuery] {
  def rowId(id: TileId, md: LayerMetaData) = new Text(s"${md.level.id}_${id}")
  val rowIdRx = """(\d+)_(\d+)""".r // (zoom)_(TmsTilingId)

  /** Map rdd of indexed tiles to tuples of (table name, row mutation) */
  def encode(raster: RasterRDD[TimeBandTile], layer: String): RDD[(Text, Mutation)] =
    raster.map{ case (TimeBandTile(tileId, time), tile) =>
      val mutation = new Mutation(rowId(tileId, raster.metaData))
      mutation.put(
        new Text(layer), new Text(time.toString),
        System.currentTimeMillis(),
        new Value(tile.toBytes()))
      (null, mutation)
    }

  /** Delegate for setting query ranges and iterators */
  def setQueryParams(job: Job, metaData: LayerMetaData, query: SpaceTimeQuery): Unit = {
    import geotrellis.spark.tiling._

    val ranges: Seq[ARange] =
      query.tileBounds match {
        case None => //No spacial query still requires a filter on zoom level
          new ARange(
            new Text(s"${metaData.level.id}_0"),
            new Text(s"${metaData.level.id}_9")
          ) :: Nil
        case Some((tileBounds, coordScheme)) =>
          metaData.transform
            .withCoordScheme(coordScheme)
            .tileToIndex(tileBounds)
            .spans
            .map { ts => new ARange(rowId(ts._1, metaData), rowId(ts._2, metaData)) }
      }
    InputFormatBase.setRanges(job, ranges)

    query.timeBounds match {
      case None =>
      case Some((startTime, endTime)) =>
        val pair = new JPair(new Text(startTime.toString), new Text(endTime.toString))
        InputFormatBase.fetchColumns(job,  pair :: Nil)
    }
  }

  /** Maps RDD of Accumulo specific Key, Value pairs to a tuple of (K, Tile) and wraps it in RasterRDD */
  def decode(rdd: RDD[(Key, Value)], metaData: LayerMetaData): RasterRDD[TimeBandTile] = {
    val tileRdd = rdd.map {
      case (key, value) =>
        val rowIdRx(zoom, id) = key.getRow.toString
        val time = key.getColumnQualifier.toString.toDouble
        val tile = ArrayTile.fromBytes(value.get,
          metaData.cellType, metaData.tileLayout.tileCols, metaData.tileLayout.tileRows)
        TimeBandTile(id.toLong, time) -> tile.asInstanceOf[Tile]
    }
    new RasterRDD(tileRdd, metaData)

  }
}
