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

package geotrellis.jetty

import geotrellis._

/**
 * Starts a webserver on the configured port that will serve any rest
 * services found in the package defined as 'rest-package' in the configuration file.
 * By default, the example services are included, found in geotrellis.jetty.service.
 * Any classes defined in an included package with JAX-RS attributes will become REST services.
 */

object WebRunner {

  def createJettyServer(config:ServerConfig) = 
    new JettyServer(config.jetty).withPackages(config.packages)

  @deprecated("Use run method.","0.8.3")
  def main(args: Array[String]):Unit =
    run({ s => })

  def run():Unit = run({s => })

  def run(configServer:JettyServer=>Unit):Unit = {
    printWelcome()

    Logger.setLogger(new ConsoleLogger())

    try {
      val config = ServerConfig.init()
      val server = createJettyServer(config)
      
      config.staticContentPath match {
        case Some(path) => 
          if(!new java.io.File(path).isDirectory) {
            Logger.log(s"ERROR - $path is not a valid directory for static content.")
            Logger.log(s"        Check the geotrellis.server.static-path property of your configuration")
            errorOut()
          }
        server.withStaticContent(path)
        case None =>
      }

      configServer(server)

      Logger.log(s"Starting server on port ${config.jetty.port}.")
      for(p <- config.packages) { Logger.log(s"\tIncluding package $p") }

      server.start()
    } finally {
      GeoTrellis.shutdown()
    }
  }

  def printWelcome() = {
    println("\n\t--=== GEOTRELLIS SERVER ===--\n")
  }

  def errorOut() = {
    System.exit(1)
  }
}

object Logger extends Logger {
  private var logger:Logger = new EmptyLogger()

  def setLogger(l:Logger) = { logger = l }

  def log(msg:String) = logger.log(msg)
  def error(msg:String) = logger.error(msg)
}

trait Logger { 
  def log(msg:String):Unit 
  def error(msg:String):Unit
}

class ConsoleLogger extends Logger { 
  def log(msg:String)   { println(s"[GEOTRELLIS]  $msg") }
  def error(msg:String) { println(s"[GEOTRELLIS - ERROR] $msg") }
}

class EmptyLogger extends Logger { 
  def log(msg:String) { } 
  def error(msg:String) { }
}
