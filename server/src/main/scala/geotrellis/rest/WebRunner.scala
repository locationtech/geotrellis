package geotrellis.rest

/**
 * Starts a webserver on the configured port that will serve any rest
 * services found in the package defined as 'rest-package' in the configuration file.
 * By default, the admin services are included, found in geotrellis.admin.services.
 * Any classes defined in an included package with JAX-RS attributes will become REST services.
 */

object WebRunner {
  def main(args: Array[String]) {
    printWelcome()

    Logger.setLogger(new ConsoleLogger())

    try {
      val config = ServerConfig.init()
      
      val server = new JettyServer(config.jetty)
      .withPackages(config.packages, "/gt/*")
      
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

      if(config.admin.serveSite && config.admin.serveFromJar) {
        server.withResourceContent("/webapp")
        Logger.log(s"Including Admin Site...")
      }

      GeoTrellis.setup(config.geotrellis)

      Logger.log(s"Starting server on port ${config.jetty.port}.")
      for(p <- config.packages) { Logger.log(s"\tIncluding package $p") }

      server.start()
    } /*catch {
      case e:Exception =>
        Logger.error(e.getMessage)
    } */finally {
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
