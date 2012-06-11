package geotrellis.rest

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.nio.SelectChannelConnector
import org.eclipse.jetty.servlet.{ServletHolder, ServletContextHandler}
import org.eclipse.jetty.util.thread.QueuedThreadPool
import com.sun.jersey.spi.container.servlet.ServletContainer

import com.typesafe.config.ConfigFactory

/**
 * Starts a webserver on the configured port (default 8080) that will serve any rest
 * services found in the package defined as 'resource_package' in the configuration file.
 *
 * At the moment, the directory is this package -- geotrellis.rest.
 * See "HelloService" for an example.  Any classes defined in the package with
 * JAX-RS attributes will become REST services.
 */

object WebRunner {
  val config = ConfigFactory.load()

  def main(args: Array[String]) {
    val host = config.getString("geotrellis.host")
    val port = config.getInt("geotrellis.port")

    println("Starting server on port %d.".format(port))
    val server = new Server()

    val connector = new SelectChannelConnector()
    connector.setHost(host)
    connector.setPort(port)
    server.addConnector(connector)

    val holder: ServletHolder = new ServletHolder(classOf[ServletContainer])
    holder.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
      "com.sun.jersey.api.core.PackagesResourceConfig")

    val pkg = config.getString("geotrellis.rest-package")
    holder.setInitParameter("com.sun.jersey.config.property.packages", pkg)

    val context = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS)
    context.addServlet(holder, "/*")
    server.start
    server.join
  }
}
