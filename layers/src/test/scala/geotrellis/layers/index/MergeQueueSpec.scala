package geotrellis.layers.index

import java.time.{ZoneId, ZonedDateTime}

import geotrellis.proj4.{LatLng, Sinusoidal}
import geotrellis.raster._
import geotrellis.tiling._
import geotrellis.layers._
import geotrellis.layers._
import geotrellis.layers.index.hilbert.HilbertSpaceTimeKeyIndex
import geotrellis.layers.index.zcurve.ZSpaceTimeKeyIndex
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

  def randomPoly(bounds: Extent, maxNumSides: Int, maxSideLength: Double) = {
    val R = Point(bounds.xmin + rgen.nextFloat()*bounds.width,
                  bounds.ymin + rgen.nextFloat()*bounds.height)


    val numSides = if(maxNumSides <= 3) 3 else rgen.nextInt(maxNumSides - 2) + 3
    val polars = for(_ <- 1 to numSides) yield (rgen.nextDouble*2.0*Math.PI, rgen.nextDouble*maxSideLength)
    polars.sortBy(_._1)
    val points = polars.map { tup =>
      val (r,theta) = tup
      val x = R.x + r*Math.cos(theta)
      val y = R.y + r*Math.sin(theta)
      val xClipped = bounds.xmin max x min bounds.xmax
      val yClipped = bounds.ymin max y min bounds.ymax
      Point(xClipped,yClipped)
    }
    Polygon(points :+ points.head)
  }

  val polys = for (i <- 1 to 1500) yield randomPoly(LatLng.worldExtent,10,.2)
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

  it("should work on high-cardinality range sets") {
    val query = new LayerQuery[SpaceTimeKey, TileLayerMetadata[SpaceTimeKey]]
      .where(Intersects(mPoly))
      .where(Between(ZonedDateTime.of(2012, 1, 1, 0, 0, 0, 0, ZoneId.of("Zulu")), ZonedDateTime.of(2012, 12, 31, 0, 0, 0, 0, ZoneId.of("Zulu"))))
    val kbs = query(md)
    val ranges = kbs.flatMap(kb => index.indexRanges(kb))
    val cnt = ranges.size
    info(s"ZIndex Count: $cnt")

    val mq = MergeQueue(ranges)
    info(s"MergeQueue Count: ${mq.size}")
  }
}
