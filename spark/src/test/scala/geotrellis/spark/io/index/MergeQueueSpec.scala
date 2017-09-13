package geotrellis.spark.io.index

import java.time.{ZoneId, ZonedDateTime}

import geotrellis.proj4.{LatLng, Sinusoidal}
import geotrellis.raster._
import geotrellis.spark.io._
import geotrellis.spark._
import geotrellis.spark.io.index.hilbert.HilbertSpaceTimeKeyIndex
import geotrellis.spark.io.index.zcurve.ZSpaceTimeKeyIndex
import geotrellis.spark.tiling._
import geotrellis.vector.io._
import geotrellis.vector.io.json.JsonFeatureCollection
import geotrellis.vector._
import org.scalatest._

class MergeQueueSpec extends FunSpec {

  //val mPoly = MultiPolygon(mPolys.flatMap(p => p.reproject(LatLng, Sinusoidal).polygons))

  val seed = 32269
  val rgen = new scala.util.Random(seed)

  def randomSquare(bounds: Extent, side: Double) = {
    val x = bounds.xmin + rgen.nextFloat()*(bounds.width - side)
    val y = bounds.ymin + rgen.nextFloat()*(bounds.height - side)
    Polygon(Point(x,y),Point(x+side,y),Point(x+side,y+side),Point(x,y+side),Point(x,y))
  }

  val polys = for (i <- 1 to 1500) yield randomSquare(LatLng.worldExtent,.5)
  val mPoly = MultiPolygon(polys)

  val worldKB = KeyBounds(SpaceTimeKey(0, 0, 1325376000000L), SpaceTimeKey(360, 180, 1355788800000L))
  val layout = LayoutDefinition(LatLng.worldExtent, TileLayout(360, 180, 240, 240))
  val md = TileLayerMetadata(
    IntCellType,
    layout,
    LatLng.worldExtent,
    LatLng,
    worldKB)

  val index = ZSpaceTimeKeyIndex.byDay(worldKB)
  //val index = HilbertSpaceTimeKeyIndex(worldKB, 21, 9)

  it("reproducing bad behavior...") {

    val query = new LayerQuery[SpaceTimeKey, TileLayerMetadata[SpaceTimeKey]]
      .where(Intersects(mPoly))
      .where(Between(ZonedDateTime.of(2012, 1, 1, 0, 0, 0, 0, ZoneId.of("Zulu")), ZonedDateTime.of(2012, 12, 31, 0, 0, 0, 0, ZoneId.of("Zulu"))))
    val kbs = query(md)
    val ranges = kbs.flatMap(kb => index.indexRanges(kb))
    val cnt = ranges.size
    println(s"ZIndex cnt: $cnt")

    val mq = MergeQueue(ranges)
    println(s"mq cnt: ${mq.size}")
  }
}
