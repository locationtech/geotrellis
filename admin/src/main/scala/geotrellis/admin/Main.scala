/**************************************************************************
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
 **************************************************************************/

package geotrellis.admin

import geotrellis._

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http

import com.typesafe.config.{ConfigFactory,Config}

object Main {
  def main(args: Array[String]):Unit = {
    val config = ConfigFactory.load()
    val host = config.getString("geotrellis.admin.host")
    val port = config.getInt("geotrellis.admin.port")
    val staticContentPath = new java.io.File(config.getString("geotrellis.admin.static-content")).getAbsolutePath
    GeoTrellis.init
    println(s"SERVERING STATIC CONTENT FROM $staticContentPath")
    try {
      implicit val system = GeoTrellis.server.system

      val service = system.actorOf(Props(classOf[AdminServiceActor],staticContentPath), "admin-service")
      IO(Http) ! Http.Bind(service, host, port = port)
    } catch {
      case _:Exception =>
        GeoTrellis.shutdown
    }
  }
}
