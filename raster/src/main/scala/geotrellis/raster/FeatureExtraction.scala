package geotrellis.raster

import geotrellis.raster.rasterize.Rasterizer
import geotrellis.vector.{Feature, Geometry, Point, PointFeature}
import spire.syntax.cfor._

import scala.collection.mutable.ListBuffer

trait FeatureExtraction[G <: Geometry, T <: CellGrid[Int], D] {
  def features(geom: G, raster: Raster[T]): Array[Array[PointFeature[D]]]
}

object FeatureExtraction {
  def apply[G <: Geometry: FeatureExtraction[*, T, D], T <: CellGrid[Int], D] = implicitly[FeatureExtraction[G, T, D]]

  implicit def multibandTile[G <: Geometry] = new FeatureExtraction[G, MultibandTile, Int] {
    def features(geom: G, raster: Raster[MultibandTile]): Array[Array[PointFeature[Int]]] = {
      val arr = Array.ofDim[Array[PointFeature[Int]]](raster.tile.bandCount)

      cfor(0)(_ < raster.tile.bandCount, _ + 1) { i =>
        val buffer = ListBuffer[PointFeature[Int]]()
        Rasterizer.foreachCellByGeometry(geom, raster.rasterExtent) { case (col, row) =>
          buffer += Feature(Point(raster.rasterExtent.gridToMap(col, row)), raster.tile.band(i).get(col, row))
        }
        arr(i) = buffer.toArray
      }

      arr
    }
  }

  implicit def multibandTileDouble[G <: Geometry] = new FeatureExtraction[G, MultibandTile, Double] {
    def features(geom: G, raster: Raster[MultibandTile]): Array[Array[PointFeature[Double]]] = {
      val arr = Array.ofDim[Array[PointFeature[Double]]](raster.tile.bandCount)

      cfor(0)(_ < raster.tile.bandCount, _ + 1) { i =>
        val buffer = ListBuffer[PointFeature[Double]]()
        Rasterizer.foreachCellByGeometry(geom, raster.rasterExtent) { case (col, row) =>
          buffer += Feature(Point(raster.rasterExtent.gridToMap(col, row)), raster.tile.band(i).getDouble(col, row))
        }
        arr(i) = buffer.toArray
      }

      arr
    }
  }

  implicit def tile[G <: Geometry] = new FeatureExtraction[G, Tile, Int] {
    def features(geom: G, raster: Raster[Tile]): Array[Array[PointFeature[Int]]] =
      multibandTile[G].features(geom, raster.mapTile(MultibandTile(_)))
  }

  implicit def tileDouble[G <: Geometry] = new FeatureExtraction[G, Tile, Double] {
    def features(geom: G, raster: Raster[Tile]): Array[Array[PointFeature[Double]]] = {
      multibandTileDouble[G].features(geom, raster.mapTile(MultibandTile(_)))
    }
  }
}
