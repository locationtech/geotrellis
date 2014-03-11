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

package geotrellis.run

import com.beust.jcommander._

import geotrellis._
import geotrellis.data._
import geotrellis.process._
import geotrellis.raster.op._
import geotrellis.io

/**
 * Gives information about an ARG raster.
 */
@Parameters(commandNames = Array("info"), commandDescription ="Provides information about an ARG raster.")
class InfoTask extends Task { 
  @Parameter( 
    names = Array("--source", "-s"),  
    description = "Path of source ARG raster.",  
    required=true) 
  var sourcePath:String = _  
 
  val taskName = "info"
 
  def execute = {
    InfoTask.execute(sourcePath)
  } 
} 

object InfoTask {
  def doesFileExist(p:String) = {
    if(!(new java.io.File(p).exists())) {
      println("File %s does not exist.".format(p))
      false
    }
    true
  }

  def execute(sourcePath:String):Unit = {  
    if(!doesFileExist(sourcePath)) { return }

    val localServer = Server.empty("task")
    
    try {
      val info = localServer.get(io.LoadRasterLayerInfoFromPath(sourcePath))
      val raster = localServer.get(io.LoadFile(sourcePath))

      println(s"ARG at $sourcePath")
      println("------------------------------------------")
      println(s"Data Type: ${info.rasterType}")
      println(s"Raster Exent: ${info.rasterExtent}")

      if(info.rasterType.isDouble) {
        val (min,max) = raster.findMinMaxDouble
        println(s"Min $min  Max $max")
        var sum = 0.0
        var count = 0
        raster.foreachDouble { z => if(isData(z)) { sum += z; count += 1; } }
        println(s"Mean: ${sum/count}")
      } else {
        val (min,max) = raster.findMinMax
        println(s"Min $min  Max $max")
        var sum = 0
        var count = 0
        raster.foreach { z => if(isData(z)) { sum += z; count += 1} }
        println(s"Mean: ${sum/count}")
      }
    } finally {
      localServer.shutdown()
    }
  }
}
