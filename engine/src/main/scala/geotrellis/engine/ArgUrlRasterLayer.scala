/*
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
 */

package geotrellis.engine

import geotrellis._
import geotrellis.raster.io.arg.ArgReader
import geotrellis.raster._
import geotrellis.util._

import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import spray.http._
import spray.client.pipelining._

import com.typesafe.config.Config

object ArgUrlRasterLayerBuilder
extends RasterLayerBuilder {
  def apply(ds: Option[String], jsonPath: String, json: Config): RasterLayer = {
    val url = 
      if(json.hasPath("url")) {
        json.getString("url")
      } else {
        throw new java.io.IOException(s"[ERROR] 'argurl' type rasters must have 'url' field in json.")
      }

    val cols = json.getInt("cols")
    val rows = json.getInt("rows")

    val (cellWidth, cellHeight) = getCellWidthAndHeight(json)
    val rasterExtent = RasterExtent(getExtent(json), cellWidth, cellHeight, cols, rows)

    val info = 
      RasterLayerInfo(
        LayerId(ds, getName(json)),
        getCellType(json),
        rasterExtent,
        getEpsg(json),
        getXskew(json),
        getYskew(json),
        getCacheFlag(json)
      )

    new ArgUrlRasterLayer(info, url)
  }
}

class ArgUrlRasterLayer(info: RasterLayerInfo, rasterUrl: String) 
extends UntiledRasterLayer(info) {
  def getRaster(targetExtent: Option[RasterExtent]) =
    if(isCached) {
      getCache.lookup[Array[Byte]](info.id.toString) match {
        case Some(bytes) =>
          fromBytes(bytes, targetExtent)
        case None =>
          sys.error("Cache problem: Layer thinks it's cached but it is in fact not cached.")
      }
    } else {
      fromBytes(getBytes, targetExtent)
    }

  def getBytes = {
    val d: Duration = 5 seconds
    implicit val t: Timeout = 5 seconds
    val (system, shutdown) =
      if(GeoTrellis.isInit)
        // If the GeoTrellis.engine is being used, use it's Actor System
        (GeoTrellis.engine.system, { () => })
      else {
        // otherwise, create a temporary one
        val s = ActorSystem(s"system-request-$rasterUrl")
        (s, () => s.shutdown)
      }

    implicit val s = system
    import s.dispatcher

    try {
      val pipeline = sendReceive ~> unmarshal[Array[Byte]]
      val response: Future[Array[Byte]] =
        pipeline(Get(rasterUrl))

      Await.result(response, d)
    } finally {
      shutdown()
    }
  }

  def cache(c: Cache[String]) = 
        c.insert(info.id.toString, getBytes)

  private def fromBytes(arr: Array[Byte], target: Option[RasterExtent]) = {
    target match {
      case Some(re) =>
        ArgReader.warpBytes(arr: Array[Byte], info.cellType, info.rasterExtent, re)
      case None =>
        ArrayTile.fromArrayByte(arr, info.cellType, info.rasterExtent.cols, info.rasterExtent.rows)
      }
  }
}
