package geotrellis.rest

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.nio.SelectChannelConnector
import org.eclipse.jetty.servlet.{ServletHolder, ServletContextHandler}
import org.eclipse.jetty.util.thread.{QueuedThreadPool,ExecutorThreadPool}
import com.sun.jersey.spi.container.servlet.ServletContainer
import java.util.concurrent.TimeUnit
import com.typesafe.config.ConfigFactory

/**
 * Starts a webserver on the configured port that will serve any rest
 * services found in the package defined as 'rest-package' in the configuration file.
 * By default, the admin services are included, found in geotrellis.admin.services.
 * Any classes defined in an included package with JAX-RS attributes will become REST services.
 */

object WebRunner {
  val config = ConfigFactory.load()

  def main(args: Array[String]) {
    printWelcome()

    val packages = Seq(config.getString("geotrellis.rest-package"),
                       "geotrellis.admin.services")

    val host = config.getString("geotrellis.host")
    val port = config.getInt("geotrellis.port")

    val corePoolSize = config.getInt("geotrellis.jetty.corePoolSize")
    val maximumPoolSize = config.getInt("geotrellis.jetty.maximumPoolSize")
    val keepAliveTime =  config.getLong("geotrellis.jetty.keepAliveMilliseconds")

    val server = new Server()
    server.setThreadPool(new ExecutorThreadPool(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.MILLISECONDS))

    val connector = new SelectChannelConnector()
    connector.setHost(host)
    connector.setPort(port)
    server.addConnector(connector)

    val holder: ServletHolder = new ServletHolder(classOf[ServletContainer])
    holder.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
                            "com.sun.jersey.api.core.PackagesResourceConfig")

    holder.setInitParameter("com.sun.jersey.config.property.packages", packages.mkString(";"))

    val context = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS)

    if(config.getBoolean("geotrellis.server.serve-static")) {
      context.setResourceBase("/home/rob/proj/gt/geotrellis-alt/server/src/main/webapp/")
      context.setWelcomeFiles(Array("index.html"))
      context.addServlet(classOf[org.eclipse.jetty.servlet.DefaultServlet], "/*")
    }

    context.addServlet(holder, "/gt/*")

    log(s"Starting server on port $port.")
    for(p <- packages) { log(s"\tIncluding package $p") }

    server.start
    server.join

  }

  def printWelcome() = {
    println("\n\t--=== GEOTRELLIS SERVER ===--\n")
  }

  def log(msg:String) = {
    println(s"[GEOTRELLIS]  $msg")
  }
}
