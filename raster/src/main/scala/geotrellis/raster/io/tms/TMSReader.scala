package geotrellis.raster.io

import geotrellis.raster._
import geotrellis.vector.{Point, Extent}
import geotrellis.spark.tiling.ZoomedLayoutScheme

import spire.syntax.cfor._
import scalaj.http._

import scala.collection.mutable._
import java.net.{URI, URL}
import javax.imageio._

class TMSReader[T](
  uriTemplate: String,
  f: URI => T = TMSReader.decodeTile _,
  tileSize: Int = ZoomedLayoutScheme.DEFAULT_TILE_SIZE
) extends Serializable {
  def uri(z: Int, x: Int, y: Int): URI = new URI(
    uriTemplate.replace("{z}", z.toString)
      .replace("{x}", x.toString)
      .replace("{y}", y.toString)
  )

  def read(zoom: Int, extent: Extent): Iterator[((Int, Int), T)] = {
    val layoutDefinition = ZoomedLayoutScheme.layoutForZoom(zoom, extent, tileSize)
    val bounds = layoutDefinition.mapTransform.extentToBounds(extent)
    // construct bounds from zoom + extent
    for ((x, y) <- bounds.coordsIter)
    yield (x, y) -> f(uri(zoom, x, y))
  }

  def read(zoom: Int, x: Int, y: Int): T = f(uri(zoom, x, y))
}

object TMSReader {
  import geotrellis.raster.render._

  // ImageIO use here to deal with different decoding behaviors (png vs jpg)
  def decodeTile(uri: URI): Tile = {
    val img = ImageIO.read(uri.toURL)
    val height = img.getHeight()
    val width = img.getWidth()
    val rgbArr = img.getRGB(0, 0, width, height, null, 0, width)
    IntArrayTile(rgbArr, width, height, None)
  }

  // ImageIO use here to deal with different decoding behaviors (png vs jpg)
  def decodeMultibandTile(uri: URI): MultibandTile = {
    val img = ImageIO.read(uri.toURL)
    val height = img.getHeight()
    val width = img.getWidth()
    val length = width * height

    val rgbArr = img.getRGB(0, 0, width, height, null, 0, width)
    val reds = new ArrayBuffer[Byte](length)
    val greens = new ArrayBuffer[Byte](length)
    val blues = new ArrayBuffer[Byte](length)
    val alphas = new ArrayBuffer[Byte](length)
    cfor(0)(_ < length, _ + 1) { cellIdx =>
      val cell = rgbArr(cellIdx)
      reds :+ cell.red.toByte
      greens :+ cell.green.toByte
      blues :+ cell.blue.toByte
      alphas :+ cell.alpha.toByte
    }

    val redTile = ByteArrayTile(reds.toArray, width, height, None)
    val greenTile = ByteArrayTile(greens.toArray, width, height, None)
    val blueTile = ByteArrayTile(blues.toArray, width, height, None)
    val alphaTile = ByteArrayTile(alphas.toArray, width, height, None)
    MultibandTile(redTile, greenTile, blueTile, alphaTile)
  }

}

