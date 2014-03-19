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

import  scala.collection._
 
// Command line tool to run geotrellis tasks.

// Use wrapper script or "start-script" task to create start script to run in sbt directory, e.g:
// ./target/start geotrellis.run.Tasks geotiff_convert -i src/test/resources/econic.tif -n econic -o /tmp/econic.arg32


/**
 * Task for command line execution.
 *
 * Each task is defined as a JCommander 'command' (http://jcommander.org/) with annotations
 * that define what command line arguments the command takes.  The idea of a command is similar 
 * to a github command, e.g. "git add" versus "git commit" ('add' and 'commit' are commands). 
 * See defined tasks under trellis.run for examples of how to define a task.  A basic example:
 *
 *  @Parameters(commandNames = Array("demo_command"), commandDescription="peachy keen command description")
 *  class DemoTask extends Task {
 *    @Parameter(
 *     names = Array("--file", "-f"),
 *     description("File for transmogrification"),
 *     required = true)
 *     var inPath:String = _
 *
 *     val taskName = "demo_command"
 *
 *     def execute = { DemoTask.transmogrify(this.inPath) }
 * }
 */
abstract class Task {
  def taskName:String
  def execute(): Unit
}


object Tasks {
  class BaseArgs {
    @Parameter(names = Array("-h", "--help"), description = "Show this help screen")
    var help: Boolean = _
    @Parameter(names = Array("--version", "-version"), description = "Show the program version")
    var version: Boolean = _
  }

  var tasks:mutable.ListBuffer[Task] = mutable.ListBuffer[Task]()

  def addTask(t:Task) = {
    tasks.append(t)
  }

  def usage(jcOpt:Option[JCommander]) = { 
     jcOpt.foreach(_.usage)
     println("----------------------------------------------------")
     println("\n * before an option indicates that it is required.")
     println("\n\nExample:")
     println("./geotrellis import -i src/test/resources/econic.tif -n econic -d /data/arg\n")

     System.exit(0)
  }

  def main(args:Array[String]):Unit = {
    run(args)
  }

  def run(args:Array[String]):Int = {
    val baseArgs = new BaseArgs()

    val jc = new JCommander()
    jc.setProgramName("geotrellis")
    jc.addObject(baseArgs)
    var taskMap = mutable.Map[String,Task]()

    // We use reflection to load all available defined tasks (which extend Task) under
    // the geotrellis.run package.
    //TODO: command line argument to specify package to search for tasks 
    //REVIEW: This reflection takes ~250ms.  Should we just define the tasks, or use configuration?
    val r = new org.reflections.Reflections("geotrellis.run")
    val s = r.getSubTypesOf(classOf[Task]).iterator
    while (s.hasNext) {
      val t = s.next
      val task = (t.newInstance).asInstanceOf[Task]
      taskMap(task.taskName) = task
      jc.addCommand(task)
    } 
      
    try {
      jc.parse(args: _*)
    } catch {
      case _:Throwable => { usage(Option(jc)) } 
    }
    
    if (baseArgs.help) {
      usage(Option(jc))
    }
  
    val c = jc.getParsedCommand()

    if (c == null) {
      usage(Option(jc))
    }

    c match {
      case taskName:String => { taskMap(taskName).execute } 
      case _ => throw new Exception("getParsedCommand didn't return a string")
    }

    0
  }

}

import xsbti.{ AppMain, AppConfiguration }

class Script extends AppMain {
  def run(conf: AppConfiguration) =
    Exit(Tasks.run(conf.arguments))
}

case class Exit(val code: Int) extends xsbti.Exit
