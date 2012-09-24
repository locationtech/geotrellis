package demo

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Response
import javax.ws.rs.{GET, Path, DefaultValue, QueryParam}
import javax.ws.rs.core.{Response, Context}

// import core geotrellis types
import geotrellis._

// import some useful operations
import geotrellis.raster.op._
import geotrellis.statistics.op._
import geotrellis.rest.op._

// import syntax implicits like "raster + raster"
import geotrellis.Implicits._

object response {
  def apply(mime:String)(data:Any) = Response.ok(data).`type`(mime).build()
}

/**
 * Operation to perform the basic weighted overlay calculation.
 */
object WeightedOverlayBasic {
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

    val rs:Op[Array[Raster]] = logic.ForEach(rasters, weights)(_ * _)

    val weightSum:Op[Int] = logic.Do(weights)(_.sum)

    local.AddArray(rs) / weightSum
  }
}

object Demo {
  val server = process.Server("demo", "src/test/resources/demo-catalog.json")

  def errorPage(msg:String, traceback:String) = """
<html>
 <p>%s</p>
 <tt>%s</tt>
</html>
""" format (msg, traceback)

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

  final val defaultBox = "-8379782.57151,4846436.32082,-8360582.57151,4865636.32082"
  final val defaultColors = "ff0000,ffff00,00ff00,0000ff"

  @GET
  def get(
    @DefaultValue(defaultBox) @QueryParam("bbox") bbox:String,
    @DefaultValue("256") @QueryParam("cols") cols:String,
    @DefaultValue("256") @QueryParam("rows") rows:String,
    @DefaultValue("SBN_inc_percap") @QueryParam("layers") layers:String,
    @DefaultValue("1") @QueryParam("weights") weights:String,
    @DefaultValue("") @QueryParam("mask") mask:String,
    @DefaultValue(defaultColors) @QueryParam("palette") palette:String,
    @DefaultValue("4") @QueryParam("colors") numColors:String,
    @DefaultValue("info") @QueryParam("format") format:String,
    @Context req:HttpServletRequest
  ) = {

    // First let's figure out what geographical area we're interestd in, as
    // well as the resolution we want to use.
    val colsOp = string.ParseInt(cols)
    val rowsOp = string.ParseInt(rows)
    val extentOp = string.ParseExtent(bbox)
    val reOp = extent.GetRasterExtent(extentOp, colsOp, rowsOp)

    // Figure out which rasters and weights the user wants to use.
    val layerOps = logic.ForEach(string.SplitOnComma(layers))(io.LoadRaster(_, reOp))
    val weightOps = logic.ForEach(string.SplitOnComma(weights))(string.ParseInt(_))

    // Do the actual weighted overlay operation
    val overlayOp = WeightedOverlayArray(layerOps, weightOps)

    // Cache and (optionally) mask the result.
    val outputOp = if (mask.isEmpty) {
      overlayOp
    } else {
      local.Mask(overlayOp, io.LoadRaster(mask, reOp), NODATA, NODATA)
    }

    // Build a histogram of the output raster values.
    val histogramOp = stat.GetHistogram(outputOp)

    // Parse the user's color palette and allocate colors.
    val paletteColorsOp = logic.ForEach(string.SplitOnComma(palette))(s => string.ParseHexInt(s))
    val numColorsOp = string.ParseInt(numColors)
    val colorsOp = stat.GetColorsFromPalette(paletteColorsOp, numColorsOp)

    // Determine some good quantile breaks to use for coloring output.
    val breaksOp = stat.GetColorBreaks(histogramOp, colorsOp)

    // Render the actual PNG image.
    val pngOp = io.RenderPng(outputOp, breaksOp, histogramOp, Literal(0))

    format match {
      case "hello" => response("text/plain")("hello world")
      case "info" => Demo.server.getResult(pngOp) match {
        case process.Complete(img, h) => {
          val ms = h.elapsedTime
          val query = req.getQueryString + "&format=png"
          val url = "/demo1?" + query + "&format=png"
          println(url)
          val html = Demo.infoPage(cols.toInt, rows.toInt, ms, url, h.toPretty)
          response("text/html")(html)
        }
        case process.Error(msg, trace) => {
          response("text/plain")("failed: %s\ntrace:\n%s".format(msg, trace))
        }
      }
      case _ => Demo.server.getResult(pngOp) match {
        case process.Complete(img, _) => response("image/png")(img)
        case process.Error(msg, trace) => {
          response("text/plain")("failed: %s\ntrace:\n%s".format(msg, trace))
        }
      }
    }
  }
}
