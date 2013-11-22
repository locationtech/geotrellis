package demo

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Response
import javax.ws.rs.{GET, Path, DefaultValue, QueryParam}
import javax.ws.rs.core.{Response, Context}

// import core geotrellis types
import geotrellis._
import geotrellis.render._
import geotrellis.render.op._
import geotrellis.source._
import geotrellis.raster.op._
import geotrellis.statistics.op._

// import syntax implicits like "raster + raster"
import geotrellis.Implicits._

object response {
  def apply(mime:String)(data:Any) = Response.ok(data).`type`(mime).build()
}

object WeightedOverlayArray {
  def apply(rasters:Seq[RasterSource], weights:Seq[Int]):RasterSource = {

//    val rs:Op[Array[Raster]] = logic.ForEach(rasters, weights)(_ * _)
    val weighted = rasters.zip(weights).map { case (r,w) => r * w }
    val weightedSum = 
      weighted.head + weighted.tail

    val sum = weights.foldLeft(0)(_+_)

    weightedSum / sum
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
    @DefaultValue("256") @QueryParam("cols") cols:Int,
    @DefaultValue("256") @QueryParam("rows") rows:Int,
    @DefaultValue("SBN_inc_percap") @QueryParam("layers") layersParam:String,
    @DefaultValue("1") @QueryParam("weights") weightsParam:String,
    @DefaultValue("") @QueryParam("mask") mask:String,
    @DefaultValue(defaultColors) @QueryParam("palette") palette:String,
    @DefaultValue("4") @QueryParam("colors") numColors:String,
    @DefaultValue("info") @QueryParam("format") format:String,
    @Context req:HttpServletRequest
  ) = {

    // First let's figure out what geographical area we're interestd in, as
    // well as the resolution we want to use.
    val extent = {
      val Array(xmin,ymin,xmax,ymax) = bbox.split(",").map(_.toDouble)
      Extent(xmin,ymin,xmax,ymax)
    }
    val re = RasterExtent(extent,cols,rows)

    val layers = layersParam.split(",").map(RasterSource(_,re))
    val weights = weightsParam.split(",").map(_.toInt)

    // Do the actual weighted overlay operation
    val overlay = WeightedOverlayArray(layers, weights)

    // Cache and (optionally) mask the result.
    val output = if (mask.isEmpty) {
      overlay
    } else {
      overlay.localMask(RasterSource(mask,re),NODATA,NODATA)
    }

    // Build a histogram of the output raster values.
    val histogram = output.histogram

    // Parse the user's color palette and allocate colors.
    val paletteColors = palette.split(",").map(Integer.parseInt(_,16))
    val colorsOp = GetColorsFromPalette(paletteColors, numColors.toInt)

    // Determine some good quantile breaks to use for coloring output.
    val breaksOp = GetColorBreaks(histogram.get, colorsOp)

    // Render the actual PNG image.
    val pngOp = RenderPng(output.get, breaksOp, 0)

    format match {
      case "hello" => response("text/plain")("hello world")
      case "info" => Demo.server.getResult(pngOp) match {
        case process.Complete(img, h) => {
          val ms = h.elapsedTime
          val query = req.getQueryString + "&format=png"
          val url = "/demo1?" + query + "&format=png"
          println(url)
          val html = Demo.infoPage(cols.toInt, rows.toInt, ms, url, h.toString)
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
