package geotrellis.spark.cmd

import com.quantifind.sumac.FieldArgs
import com.quantifind.sumac.ArgMain
import org.apache.hadoop.fs.Path
import geotrellis.spark.utils.SparkUtils
import geotrellis.spark.rdd.RasterHadoopRDD
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.metadata.PyramidMetadata
import geotrellis.data.GeoTiffWriter
import geotrellis.spark.tiling.TmsTiling
import geotrellis.Raster
import geotrellis.RasterExtent
import geotrellis.Extent
import org.apache.spark.Logging

object Export extends ArgMain[CommandArguments] with Logging {

  def main(args: CommandArguments) {
    val rasterPath = new Path(args.input)
    val zoom = args.zoom
    val outputDir = args.output
    val rasterPathWithZoom = new Path(rasterPath, zoom.toString)
    
    val sc = SparkUtils.createSparkContext("local", "Export", "/geotrellis-spark/target/scala-2.10/geotrellis-spark_2.10-0.10.0-SNAPSHOT.jar")
    val meta = PyramidMetadata(rasterPath, sc.hadoopConfiguration)
    println(meta)
    val (tileSize, rasterType) = (meta.tileSize, meta.rasterType)
    val raster = RasterHadoopRDD(sc, rasterPathWithZoom.toUri.toString)
   
    raster.foreach {
      case (tw, aw) => {
        val tileId = tw.get
        val (tx, ty) = TmsTiling.tileXY(tileId, zoom)
        val bounds = TmsTiling.tileToBounds(tx, ty, zoom, tileSize)
        val raster = Raster(ArgWritable.toRasterData(aw, rasterType, tileSize, tileSize),
          RasterExtent(Extent(bounds.w, bounds.s, bounds.e, bounds.n), tileSize, tileSize))
          
        GeoTiffWriter.write(s"${outputDir}/tile-${tileId}.tif", raster, meta.nodata)
        logInfo(s"---------tx: $tx, ty: $ty file: tile-${tileId}.tif")
      }
    }

    logInfo(s"Exported ${raster.count} tiles to $outputDir")
    sc.stop
  }
}