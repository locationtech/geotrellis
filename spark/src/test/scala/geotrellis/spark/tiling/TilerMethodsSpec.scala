package geotrellis.spark.tiling

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.proj4._

import geotrellis.spark._
import geotrellis.spark.tiling._

import org.scalatest._
import org.apache.spark._
import org.apache.spark.rdd._

// Defined here because of serialization
class IntTilerKeyMethods(val self: Int, extents: List[Extent]) extends TilerKeyMethods[Int, SpatialKey] {
  def extent = extents(self - 1)
  def translate(k: SpatialKey): SpatialKey = k
}

class TilerMethodsSpec extends FunSpec
  with Matchers
  with TestEnvironment
{

  describe("Tiler") {
    it("should tile overlapping rasters"){
      def createTile(value: Int): Tile =
        ArrayTile(Array.ofDim[Int](4 * 5).fill(value), 4, 5)

      val extents =
        List(
          Extent(20.0, 75.0, 100.0, 175.0),
          Extent(60.0, 25.0, 140.0, 125.0)
        )

      val (tile1, extent1) = (createTile(1), extents(0))
      val (tile2, extent2) = (createTile(2), extents(1))

      val totalExtent = Extent(0.0, 0.0, 160.0, 200.0)

      val tileLayout = TileLayout(4, 4, 4, 5)

      val layoutDefinition = LayoutDefinition(totalExtent, tileLayout)

      val mapTransform = MapKeyTransform(totalExtent, tileLayout.layoutCols, tileLayout.layoutRows)

      implicit val tm: Int => TilerKeyMethods[Int, SpatialKey] = i => new IntTilerKeyMethods(i, extents)
      val rdd: RDD[(Int, Tile)] = sc.parallelize(Array( (1, tile1), (2, tile2) ))
      val tiled =
        rdd.cutTiles(IntConstantNoDataCellType, layoutDefinition)
          .reduceByKey { case (tile1, tile2) => if(tile1.get(0,0) > tile2.get(0,0)) tile2.merge(tile1) else tile1.merge(tile2) }
          .collect
          .toMap

      tiled.size should be (4*4 - 2)

      val n = NODATA
      tiled( SpatialKey(1,2) ).toArray should be (
        Array(
          1, 1, 2, 2,
          1, 1, 2, 2,
          1, 1, 2, 2,
          n, n, 2, 2,
          n, n, 2, 2)
      )

      tiled( SpatialKey(1,1) ).toArray should be (
        Array(
          1, 1, 1, 1,
          1, 1, 1, 1,
          1, 1, 2, 2,
          1, 1, 2, 2,
          1, 1, 2, 2)
      )
    }
  }
}
