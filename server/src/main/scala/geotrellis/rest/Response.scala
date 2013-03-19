package geotrellis.rest

import geotrellis.process.Failure

import javax.ws.rs.core.{Response => XResp}
import javax.ws.rs.core.{Context, MediaType, MultivaluedMap}

object ResponseType {
  val Text = "text/plain"
  val Html = "text/html"
  val Json = "application/json"
  val Png  = "image/png"
}

trait Response {
  def setHeader(r:XResp.ResponseBuilder):XResp.ResponseBuilder = {
    r.header("Access-Control-Allow-Origin", "*")
     .header("Access-Control-Allow-Credentials", "true")
  }

  def error() = 
    setHeader(XResp.serverError().`type`("text/plain"))

  def ok(t:String) = 
    if(t == ResponseType.Png) {
      XResp.ok().`type`(t)
    } else {
      setHeader(XResp.ok().`type`(t))
    }
}

object OK extends Response {
  def apply(data:Object) = 
    ok(ResponseType.Html).entity(data)
                         .build()
  
  def png(png:Array[Byte]) = 
    ok(ResponseType.Png).entity(png)
                        .build()

  def json(json:String) = 
    ok(ResponseType.Json).entity(json)
                         .build()
}

object ERROR extends Response {
  def apply(message:String) = 
    error().entity(message)
           .build()

  def apply(message:String, trace:Failure) = 
    error().entity(message + " " + trace)
           .build()
  
}
