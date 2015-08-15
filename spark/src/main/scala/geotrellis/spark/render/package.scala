package geotrellis.spark

import geotrellis.raster.Tile
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.render._
import geotrellis.spark.io.s3._
import geotrellis.spark.io.hadoop._
import java.net.URI

package object render {
  private def fill(template: String, id: LayerId, key: SpatialKey) = template
    .replace("(x)", key.col.toString)
    .replace("(y)", key.row.toString)
    .replace("(z)", id.zoom.toString)
    .replace("(name)", id.name)

  implicit class SpatialRasterRDDRenderMethods(rdd: RasterRDD[SpatialKey]) {
    /**
     * Renders and saves each tile as a PNG.
     *
     * @param id            LayerId required to fill the zoom variable
     * @param pathTemplate  path template for each file. (ex: "s3://tile-bucket/{name}/{z}/{x}/{y}.png")
     * @param breaks        If not defined cells are assumed to be RGBA values
     */
    def renderPng(id: LayerId, pathTemplate: String, breaks: Option[ColorBreaks] = None): Unit = {
      // '(' and ')' are oddly enough valid URI characters that parser will not fail
      val parseFriendlyTemplate = pathTemplate.replace("{","(").replace("}",")")
      val uri = new URI(parseFriendlyTemplate)

      val paintTile= (k: SpatialKey, t: Tile) => breaks.fold(t.renderPng())( b => t.renderPng(b)).bytes

      // Hadoop appears to have  poor support for S3, requiring  specialized handling
      if (uri.getScheme == "s3")
        rdd.saveToS3(uri.getAuthority, key => fill(uri.getPath, id, key).replaceFirst("/",""), paintTile)
      else
        rdd.saveToHadoop(uri, key => fill(parseFriendlyTemplate, id, key), paintTile)
    }

    /**
     * Renders and saves each tile as a GeoTiff.
     *
     * @param id            LayerId required to fill the zoom variable
     * @param pathTemplate  path template for each file. (ex: "s3://tile-bucket/{name}/{z}/{x}/{y}.tiff")
     */
    def renderGeoTiff(id: LayerId, pathTemplate: String): Unit = {
      // '(' and ')' are oddly enough valid URI characters that parser will not fail
      val parseFriendlyTemplate = pathTemplate.replace("{","(").replace("}",")")
      val uri = new URI(parseFriendlyTemplate)


      val transform = rdd.metaData.mapTransform
      val crs = rdd.metaData.crs
      val paintTile= (k: SpatialKey, t: Tile) => GeoTiff(t, transform(k), crs).toByteArray

      // Hadoop appears to have  poor support for S3, requiring  specialized handling
      if (uri.getScheme == "s3")
        rdd.saveToS3(uri.getAuthority, key => fill(uri.getPath, id, key).replaceFirst("/",""), paintTile)
      else
        rdd.saveToHadoop(uri, key => fill(parseFriendlyTemplate, id, key), paintTile)
    }
  }
}
