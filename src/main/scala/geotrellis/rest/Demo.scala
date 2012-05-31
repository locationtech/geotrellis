package geotrellis.rest

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Response
import javax.ws.rs.{GET, Path, DefaultValue, QueryParam}
import javax.ws.rs.core.{Response, Context}

import geotrellis._
import geotrellis.data.MultiColorRangeChooser
import geotrellis.operation._
import geotrellis.process._
import geotrellis.Implicits._

/**
 * Operation to perform the basic weighted overlay calculation.
 */
object WeightedOverlay2 {
  def apply(raster1:Op[Raster], weight1:Op[Int],
            raster2:Op[Raster], weight2:Op[Int]) = {

    val x:Op[Raster] = raster1 * weight1
    val y:Op[Raster] = raster2 * weight2
    val z:Op[Raster] = x + y

    val weightSum:Op[Int] = weight1 + weight2

    z / weightSum
  }
}

object WeightedOverlayArray {
  def apply(rasters:Op[Array[Raster]], weights:Op[Array[Int]]) = {

    val rs:Op[Array[Raster]] = ForEach2(rasters, weights)(_ * _)

    val weightSum:Op[Int] = Map1(weights)(_.sum)

    AddArray(rs) / weightSum
  }
}

object Demo {
  val catalogPath = "src/test/resources/demo-catalog.json"
  val catalog = Catalog.fromPath(catalogPath)
  val server = Server("demo", catalog)

  def infoPage(cols:Int, rows:Int, ms:Long, url:String, tree:String) = """
<html>
<head>
 <script type="text/javascript">
 </script>
</head>
<body>
 <h2>raster time!</h2>

 <h3>rendered %dx%d image (%d pixels) in %d ms</h3>

 <table>
  <tr>
   <td style="vertical-align:top"><img style="vertical-align:top" src="%s" /></td>
   <td><pre>%s</pre></td>
  </tr>
 </table>

</body>
</html>
""" format(cols, rows, cols * rows, ms, url, tree)
}

@Path("/demo1")
class DemoService1 {

  @GET
  def get(
    @DefaultValue("-8379782.57151,4846436.32082,-8360582.57151,4865636.32082")
    @QueryParam("bbox")
    bbox:String,

    @DefaultValue("256")
    @QueryParam("cols")
    cols:String,

    @DefaultValue("256")
    @QueryParam("rows")
    rows:String,

    @DefaultValue("")
    @QueryParam("layers")
    layers:String,

    @DefaultValue("")
    @QueryParam("weights")
    weights:String,

    @DefaultValue("")
    @QueryParam("mask")
    mask:String,

    @DefaultValue("ff0000,ffff00,00ff00,0000ff")
    @QueryParam("palette")
    palette:String,

    // the requested output format
    @DefaultValue("info")
    @QueryParam("format")
    format:String,

    @Context req:HttpServletRequest
  ) = {

    /**
     * First let's figure out what geographical area we're interestd in, as
     * well as the resolution we want to use.
     */
    val colsOp = ParseInt(cols)
    val rowsOp = ParseInt(rows)
    val extentOp = ParseExtent(bbox)
    val reOp = BuildRasterExtent(extentOp, colsOp, rowsOp)

    /**
     * Figure out which rasters and weights the user wants to use.
     */
    val layerOps = ForEach(SplitOnComma(layers))(LoadRaster(_, reOp))
    val weightOps = ForEach(SplitOnComma(weights))(ParseInt(_))

    /**
     * Do the actual weighted overlay operation
     */
    val overlayOp = WeightedOverlayArray(layerOps, weightOps)

    /**
     * Cache and (optionally) mask the result.
     */
    val outputOp = if (mask.isEmpty) {
      overlayOp
    } else {
      Mask(overlayOp, LoadRaster(mask, reOp), NODATA, NODATA)
    }

    /**
     * Build a histogram of the output raster values.
     */
    val histogramOp = BuildMapHistogram(outputOp)

    /**
     * Parse the user's color palette and allocate colors.
     */
    val paletteColorsOp = ForEach (SplitOnComma (palette))(Integer.parseInt(_, 16))
    val colorsOp = ColorsFromPalette(paletteColorsOp, 12)

    /**
     * Determine some good quantile breaks to use for coloring output.
     */
    val breaksOp = FindColorBreaks(histogramOp, colorsOp)

    /**
     * Render the acutal PNG image.
     */
    //val pngOp = RenderPNG(outputOp, breaksOp, 0, true)
    val pngOp = RenderPNG(outputOp, breaksOp, 0, true)

    if (format == "hello") {
      Response.ok("hello world").`type`("text/plain").build()
    } else if (format == "info") {
      Demo.server.getResult(pngOp) match {
        case Complete(img, h) => {
          val ms = h.elapsedTime
          val query = req.getQueryString
          val url = "/demo1?" + query.replaceAll("format=info", "format=png")
          val tree = h.toPretty

          val html = Demo.infoPage(cols.toInt, rows.toInt, ms, url, tree)
          Response.ok(html).`type`("text/html").build()
        }

        case Error(msg, trace) => {
          val output = "failed: %s\n\ntrace:\n%s".format(msg, trace)
          Response.ok(output).`type`("text/plain").build()
        }
      }
    } else {
      Demo.server.getResult(pngOp) match {
        case Complete(img, _) => Response.ok(img).`type`("image/png").build()
      
        case Error(msg, trace) => {
          val output = "failed: %s\n\ntrace:\n%s".format(msg, trace)
          Response.ok(output).`type`("text/plain").build()
        }
      }
    }
  }
}
