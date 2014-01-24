package geotrellis.spark.cmd
import geotrellis._
import geotrellis.Extent
import geotrellis.Raster
import geotrellis.RasterExtent
import geotrellis.data.GeoTiffWriter
import geotrellis.raster.MutableRasterData
import geotrellis.raster.RasterData
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.metadata.PyramidMetadata
import geotrellis.spark.rdd.RasterHadoopRDD
import geotrellis.spark.tiling.TmsTiling
import geotrellis.spark.utils.SparkUtils
import org.apache.hadoop.fs.Path
import org.apache.spark.Logging
import com.quantifind.sumac.ArgMain
import java.io.File

/**
 * @author akini
 *
 * Export a raster as GeoTIFF. Each tile is saved as a separate TIFF file. Uses Spark to read the tiles
 * from the underlying raster
 *
 * Export --input <path-to-raster> --zoom <zoom> --output <path-to-dir> --sparkMaster <spark-master-ip>
 *
 * e.g., Export --input file:///tmp/all-ones --zoom 10 --output /tmp/all-ones-ingested --sparkMaster "local"
 *
 * Constraints:
 *
 * --input <path-to-raster> - this can be either on hdfs (hdfs://) or local fs (file://) and is a fully
 * qualified path to the raster pyramid
 *
 * --output <path-to-dir> - the output directory has to exist on the local file system. If the directory
 * doesn't exist, an error woud be thrown
 *
 * --sparkMaster <spark-master-ip> - this is the conventional spark cluster url
 * 	(e.g. spark://host:port, local, local[4])
 *
 */
object Export extends ArgMain[CommandArguments] with Logging {

  def main(args: CommandArguments) {
    val rasterPath = new Path(args.input)
    val zoom = args.zoom
    val rasterPathWithZoom = new Path(rasterPath, zoom.toString)
    val outputDir = args.output
    val sparkMaster = args.sparkMaster

    logInfo(s"Deleting and creating output directory: $outputDir")
    val dir = new File(outputDir)
    dir.delete()
    dir.mkdirs()

    val sc = SparkUtils.createSparkContext(sparkMaster, "Export")
    val meta = PyramidMetadata(rasterPath, sc.hadoopConfiguration)
    val (tileSize, rasterType) = (meta.tileSize, meta.rasterType)
    val raster = RasterHadoopRDD(sc, rasterPathWithZoom.toUri.toString)

    raster.foreach {
      case (tw, aw) => {
        val tileId = tw.get
        val (tx, ty) = TmsTiling.tileXY(tileId, zoom)
        val bounds = TmsTiling.tileToBounds(tx, ty, zoom, tileSize)
        val rd = ArgWritable.toRasterData(
          ArgWritable(aw.getBytes().slice(0, tileSize * tileSize * rasterType.bytes)),
          rasterType, tileSize, tileSize)
        val trd = NoDataHandler.removeGtNodata(rd, meta.nodata)
        val raster = Raster(trd, RasterExtent(Extent(bounds.w, bounds.s, bounds.e, bounds.n), tileSize, tileSize))

        GeoTiffWriter.write(s"${outputDir}/tile-${tileId}.tif", raster, meta.nodata)
        logInfo(s"---------tx: $tx, ty: $ty file: tile-${tileId}.tif")
      }
    }

    logInfo(s"Exported ${raster.count} tiles to $outputDir")
    sc.stop
  }
}