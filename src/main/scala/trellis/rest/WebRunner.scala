package trellis.rest

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.nio.SelectChannelConnector
import org.eclipse.jetty.servlet.{ServletHolder, ServletContextHandler}
import org.eclipse.jetty.util.thread.QueuedThreadPool
import com.sun.jersey.spi.container.servlet.ServletContainer


/**
 * Starts a webserver on the configured port (default 8080) that will serve any rest
 * services found in the package defined as 'resource_package' in the configuration file.
 *
 * At the moment, the directory is this package -- trellis.rest.
 * See "HelloService" for an example.  Any classes defined in the package with
 * JAX-RS attributes will become REST services.
 */

object WebRunner {

  def main(args: Array[String]) {
    var port = 8080

    println("Starting server on port %d.".format(port))
    val server = new Server()

    val connector = new SelectChannelConnector()
    connector.setHost("0.0.0.0")
    connector.setPort(port)
   // connector.setThreadPool(new QueuedThreadPool(40))
   // connector.setName("trellis")
    server.addConnector(connector)

    val holder: ServletHolder = new ServletHolder(classOf[ServletContainer])
    holder.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
      "com.sun.jersey.api.core.PackagesResourceConfig")
    holder.setInitParameter("com.sun.jersey.config.property.packages", "trellis.rest")
    val context = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS)
    context.addServlet(holder, "/*")
    server.start
    server.join
  }
}

