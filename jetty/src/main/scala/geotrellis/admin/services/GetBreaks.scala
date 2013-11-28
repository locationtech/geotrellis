package geotrellis.admin.services

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Response
import javax.ws.rs._
import javax.ws.rs.core.{Response, Context, MediaType, MultivaluedMap}
import geotrellis._
import geotrellis.admin._
import geotrellis.raster.op._
import geotrellis.statistics.op._
import geotrellis.rest._
import geotrellis.rest.op._
import geotrellis.raster._

case class ClassBreaksToJson(b:Op[Array[Int]]) extends Op1(b)({
  breaks => 
    val breaksArray = breaks.mkString("[", ",", "]")
    Result(s"""{ "classBreaks" : $breaksArray }""")
})

@Path("/admin/breaks") 
class GetBreaks {
  @GET
  def get(
    @DefaultValue("") @QueryParam("layer") layer:String,
    @DefaultValue("10") @QueryParam("numBreaks") numBreaks:String,
    @Context req:HttpServletRequest
  ):Response = {
    val layerOp = io.LoadRaster(layer)

    val numBreaksOp = string.ParseInt(numBreaks)
    val histo = stat.GetHistogram(layerOp)
    val classBreaks = stat.GetClassBreaks(histo, numBreaksOp)

    GeoTrellis.run(classBreaks) match {
      case process.Complete(breaks,_) =>
        OK.json(Json.classBreaks(breaks))
          .allowCORS()
      case process.Error(message,history) =>
        ERROR(message,history)
    }
  }
}
