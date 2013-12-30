package demo

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Response
import javax.ws.rs.{GET, Path, DefaultValue, QueryParam}
import javax.ws.rs.core.{Response, Context}

// import core geotrellis types
import geotrellis._
import geotrellis.render._
import geotrellis.source._

object response {
  def apply(mime:String)(data:Any) = Response.ok(data).`type`(mime).build()
}

object WeightedOverlayArray {
  def apply(rasters:Seq[RasterSource], weights:Seq[Int]):RasterSource = {

    val weightedSum = 
      rasters.zip(weights).map { case (r,w) => r * w }
             .localAdd

    val sum = weights.foldLeft(0)(_+_)

    weightedSum / sum
  }
}

object Demo {
  def errorPage(msg:String, traceback:String) = s"""
<html>
 <p>$msg</p>
 <tt>$traceback</tt>
</html>
"""

  def infoPage(cols:Int, rows:Int, ms:Long, url:String, tree:String) = s"""
<html>
<head>
 <script type="text/javascript">
 </script>
</head>
<body>
 <h2>raster time!</h2>

 <h3>rendered ${cols}x${rows} image (${cols*rows} pixels) in $ms ms</h3>

 <table>
  <tr>
   <td style="vertical-align:top"><img style="vertical-align:top" src="$url" /></td>
   <td><pre>$tree</pre></td>
  </tr>
 </table>

</body>
</html>
"""
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
    @DefaultValue("10") @QueryParam("colors") numColors:Int,
    @DefaultValue("info") @QueryParam("format") format:String,
    @Context req:HttpServletRequest
  ):Response = {

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

    // Optionally mask the result.
    val output = if (mask.isEmpty) {
      overlay
    } else {
      overlay.localMask(RasterSource(mask,re),NODATA,NODATA)
    }

    val paletteColors = palette.split(",").map(Integer.parseInt(_,16))

    val png:ValueSource[Png] = output.renderPng(paletteColors, numColors)

    format match {
      case "hello" => 
        response("text/plain")("hello world")
      case "info" => png.run match {
        case process.Complete(img, h) =>
          val ms = h.elapsedTime
          val url = s"demo1?format=png&${req.getQueryString}"
          println(url)
          val html = Demo.infoPage(cols.toInt, rows.toInt, ms, url, h.toString)
          response("text/html; charset=UTF-8")(html)
        case process.Error(msg, trace) => {
          response("text/plain")("failed: %s\ntrace:\n%s".format(msg, trace))
        }
      }
      case _ => png.run match {
        case process.Complete(img, _) => 
          response("image/png")(img)
        case process.Error(msg, trace) =>
          response("text/plain")("failed: %s\ntrace:\n%s".format(msg, trace))
      }
    }
  }
}
