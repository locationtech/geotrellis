/***
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***/

package geotrellis.jetty

import javax.ws.rs.core.{Response => XResp}
import javax.ws.rs.core.{Context, MediaType, MultivaluedMap, CacheControl}

import scala.language.implicitConversions

object ResponseType {
  val Text = "text/plain"
  val Html = "text/html"
  val Json = "application/json"
  val Png  = "image/png"
}

object Response {
  implicit def response2JerseyResponse(resp:Response) = {
    resp.build()
  }

  def apply(rb:XResp.ResponseBuilder) = new Response(rb)
  
  def error() = 
    Response(XResp.serverError()).mimeType("text/plain")

  def ok(t:String) = {
    Response(XResp.ok()).mimeType(t)
  }
}

class Response(private var rb:XResp.ResponseBuilder) {
  def mimeType(t:String) = {
    rb = rb.`type`(t)
    this
  }

  def data(d:Object) = {
    rb = rb.entity(d)
    this
  }

  def allowCORS() = {
    rb = rb.header("Access-Control-Allow-Origin", "*")
           .header("Access-Control-Allow-Credentials", "true")
    this
  }

  def cache(seconds:Int = 1200) = {
    val cc = new CacheControl()
    cc.setMaxAge(seconds)
    cc.setNoCache(false)
    rb = rb.cacheControl(cc)
    this
  }

  def build() = {
    rb.build()
  }
}

object OK {
  def apply(data:Object) = 
    Response.ok(ResponseType.Html).data(data)
  
  def png(png:Array[Byte]) = 
    Response.ok(ResponseType.Png).data(png)

  def json(json:String) = 
    Response.ok(ResponseType.Json).data(json)
}

object ERROR {
  def apply(message:String) = 
    Response.error().data(message)

  def apply(message:String, trace:String) = 
    Response.error().data(message + " " + trace)
}
