package geotrellis.spark.testkit

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.index._
import org.apache.spark._
import geotrellis.raster.io.geotiff.reader._
import geotrellis.vector._
import geotrellis.proj4._
import geotrellis.spark.tiling._
import geotrellis.raster.mosaic._
import scala.reflect._
import org.apache.spark._
import com.github.nscala_time.time.Imports._
import scala.collection.mutable

object RasterRDDBuilders extends RasterRDDBuilders

trait RasterRDDBuilders {
  def defaultCrs = LatLng
  def defaultLayoutSceheme = ZoomedLayoutScheme(tileSize = 256)

  /**
   * Cuts up a tile to fit a given TileLayout. 
   * 
   * However, The resulting tileset can only accuratly express the original tile if the tile diemsions
   * are exact multiple of the layout columns. For instance, when tiling a 256x256 tile into a TileLayout with 4 columns and 3 rows each of the 
   * layout tiles must have 256/3 = 85.33_ pixels. Choosing 85 pixels crops the original tile, while choosing 86 creates NoData pixels.
   *
   * In this method we choose to crop. This must be kept in mind when comparing the result of operations on the tileset vs operations on source tile.
   */
  def cutTile(input: Tile, extent: Extent, crs: CRS, tileLayout: TileLayout): Array[(SpatialKey, Tile)] = {  
    val worldExtent = crs.worldExtent
    
    val metaData = RasterMetaData(input.cellType, extent, crs, tileLayout)
    val outputMapTransform = metaData.mapTransform

    val tile = 
      if (tileLayout.totalCols.toInt != input.cols || tileLayout.totalRows.toInt != input.rows)
        input.crop(tileLayout.totalCols.toInt, tileLayout.totalRows.toInt)
      else
        input

    val tiles = 
      for {
        (col, row) <- outputMapTransform(extent).coords // iterate over all tiles we should have for input extent in the world layout
      } yield {
        val key = SpatialKey(col, row)
        val worldTileExtent = outputMapTransform(key)
        val outTile = ArrayTile.empty(tile.cellType, tileLayout.tileCols, tileLayout.tileRows)       
        outTile.merge(worldTileExtent, extent, tile)
        key -> (outTile: Tile)
      }

    tiles
  }

  /** Create RasterRDD from single band GeoTiff */
  def createRasterRDD(path: String)(implicit sc: SparkContext): RasterRDD[SpatialKey] = {
    val tif = GeoTiffReader.readSingleBand(path)
    createRasterRDD(tif.tile, tif.extent, tif.crs)
  }

  def createRasterRDD(tile: Tile, extent: Extent, crs: CRS = LatLng, layoutScheme: LayoutScheme = ZoomedLayoutScheme(tileSize = 256))
  (implicit sc: SparkContext): RasterRDD[SpatialKey] = {
    val layoutLevel: LayoutLevel = layoutScheme.levelFor(crs.worldExtent, CellSize(extent, tile.cols, tile.rows))
    createRasterRDD(tile, extent, crs, layoutLevel.tileLayout) 
  }
  
  def createRasterRDD(tile: Tile, extent: Extent, tileLayout: TileLayout)(implicit sc: SparkContext): RasterRDD[SpatialKey] = {
    createRasterRDD(tile, extent, defaultCrs, tileLayout)
  }

  def createRasterRDD(sc: SparkContext, tile: Tile, tileLayout: TileLayout): RasterRDD[SpatialKey] = {
    createRasterRDD(tile, defaultCrs.worldExtent, tileLayout)(sc)
  }

  def createRasterRDD(input: Tile, extent: Extent, crs: CRS, tileLayout: TileLayout)(implicit sc: SparkContext): RasterRDD[SpatialKey] = {      
    new RasterRDD(
      sc.parallelize(cutTile(input, extent, crs, tileLayout)), 
      RasterMetaData(input.cellType, extent, crs, tileLayout))
      }


  def createSpaceTimeRasterRDD(
    sc: SparkContext,
    tiles: Seq[(Tile, DateTime)],
    tileLayout: TileLayout,
    cellType: CellType = TypeInt
  ): RasterRDD[SpaceTimeKey] = {
    createSpaceTimeRasterRDD(tiles, defaultCrs.worldExtent, defaultCrs, tileLayout, cellType)(sc)
  }

  def createSpaceTimeRasterRDD(tiles: Seq[(Tile, DateTime)], extent: Extent, tileLayout: TileLayout, cellType: CellType)
  (implicit sc: SparkContext): RasterRDD[SpaceTimeKey] = 
    createSpaceTimeRasterRDD(tiles, extent, defaultCrs, tileLayout, cellType)

  def createSpaceTimeRasterRDD(tiles: Seq[(Tile, DateTime)], tileLayout: TileLayout, cellType: CellType)
  (implicit sc: SparkContext): RasterRDD[SpaceTimeKey] = 
    createSpaceTimeRasterRDD(tiles, defaultCrs.worldExtent, defaultCrs, tileLayout, cellType)

  def createSpaceTimeRasterRDD(
    tiles: Seq[(Tile, DateTime)],
    extent: Extent,
    crs: CRS,
    tileLayout: TileLayout,
    cellType: CellType)
  (implicit sc: SparkContext): RasterRDD[SpaceTimeKey] = {
    val tmsTiles = 
      tiles
        .flatMap { case (tile, date) =>        
          cutTile(tile, extent, crs, tileLayout)
            .map { case (key, tile) => 
              SpaceTimeKey(key, TemporalKey(date)) -> tile
    }
    }

    new RasterRDD(
      sc.parallelize(tmsTiles), 
      RasterMetaData(cellType, extent, crs, tileLayout))
  }
}
