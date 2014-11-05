package geotrellis.spark

import geotrellis.raster._
import geotrellis.spark.ingest._
import geotrellis.vector.Extent
import org.apache.spark.SparkContext._
import org.apache.spark._
import org.apache.spark.rdd._

import scala.reflect.ClassTag

trait RasterRDDSpatialMethods {
  val rdd: RasterRDD[SpatialKey]

  def concat: CompositeTile = {
    val tileMap = rdd.collect.toMap
    val rmd = rdd.metaData
    val pixelCols = rmd.tileLayout.pixelCols
    val pixelRows = rmd.tileLayout.pixelRows
    
    //discover what I have here, in reality RasterMetaData should refelect this already    
    val te = GridBounds.envelope(tileMap.keys)    
    val tiles = te.coords map { case (col, row) => 
      tileMap.getOrElse(col -> row, EmptyTile(rmd.cellType, pixelCols, pixelRows))
    }
    CompositeTile(tiles, TileLayout(te.width, te.height, pixelCols, pixelRows))    
  }
}