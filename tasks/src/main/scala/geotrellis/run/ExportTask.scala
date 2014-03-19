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

package geotrellis.run

import com.beust.jcommander._

import geotrellis._
import geotrellis.data._
import geotrellis.process._
import geotrellis.raster.op._
import geotrellis.io

import java.io._

/**
 * Task that exports a raster in arg format to a human readable form
 */
@Parameters(commandNames = Array("export"), commandDescription ="Exports raster data.")
class ExportTask extends Task { 
  @Parameter( 
    names = Array("--source", "-s"),  
    description = "Path of source ARG raster",  
    required=true) 
  var sourcePath:String = _  
 
  @Parameter( 
    names = Array("--target", "-t"),   
    description = "Path of target csv raster representation.",
    required=true) 
  var targetPath:String = _ 
 
  val taskName = "export"
 
  def execute = {
    ExportTask.execute(sourcePath, targetPath)
  } 
} 

object ExportTask {
  def doesFileExist(p:String) = {
    if(!(new File(p).exists())) {
      println("File %s does not exist.".format(p))
      false
    }
    true
  }

  def printToFile(f: File)(op: PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

  def execute(sourcePath:String, targetPath:String):Unit = {  
    if(!doesFileExist(sourcePath)) { return }

    val localServer = Server.empty("task")
    
    try {
      val r = localServer.get(io.LoadFile(sourcePath))
      var x = 0 
      var y = 0
      printToFile(new File(targetPath)) { p =>
        while(y < r.rows) {
          x = 0
          while(x < r.cols) {
            p.print(s"${r.get(x,y)}")
            x += 1
            if(x != r.cols) { p.print(",") }
          }
          p.print("\n")
          y += 1
        }
     }

      println(s"Wrote to ${targetPath}")
    } finally {
      localServer.shutdown()
    }
  }
}
