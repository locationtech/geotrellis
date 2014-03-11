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

package geotrellis.jetty

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.nio.SelectChannelConnector
import org.eclipse.jetty.util.thread.{QueuedThreadPool,ExecutorThreadPool}
import com.sun.jersey.spi.container.servlet.ServletContainer
import java.util.concurrent.TimeUnit
import org.eclipse.jetty.util.resource.ResourceCollection
import org.eclipse.jetty.servlet.{ServletHolder, 
                                  ServletContextHandler,
                                  DefaultServlet}

import scala.collection.mutable

/**
 * Starts a webserver on the configured port that will serve any rest
 * services found in the package defined as 'rest-package' in the configuration file.
 * By default, the admin services are included, found in geotrellis.admin.services.
 * Any classes defined in an included package with JAX-RS attributes will become REST services.
 */

class JettyServer(config:JettyConfig) {
  val server = new Server()
  val threadPool = new ExecutorThreadPool(config.corePoolSize, 
                                          config.maximumPoolSize, 
                                          config.keepAliveTime, 
                                          TimeUnit.MILLISECONDS)

  val context = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS)

  server.setThreadPool(threadPool)

  val connector = new SelectChannelConnector()
  connector.setHost(config.host)
  connector.setPort(config.port)
  server.addConnector(connector)

  val staticResources = mutable.ListBuffer[String]()
  
  def withPackages(packages:Seq[String], initParameters:Map[String,String]=Map()) = {
    val holder = new ServletHolder(classOf[ServletContainer])
    holder.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
                            "com.sun.jersey.api.core.PackagesResourceConfig")
    holder.setInitParameter("com.sun.jersey.config.property.packages", packages.mkString(";"))

    initParameters foreach {
      case (key, value) => holder.setInitParameter(key, value)
    }

    context.addServlet(holder, config.contextPath)
    this
  }
  
  def withStaticContent(staticPath:String) = {
    Logger.log(s"Serving static content from $staticPath")
    staticResources.insert(0,staticPath)
    this
  }

  def withResourceContent(resourcePath:String) = {
    val path = getClass.getResource(resourcePath).toExternalForm()
    staticResources.insert(0,path)
    this
  }
  
  def start() = {
    if(!staticResources.isEmpty) {
      context.setBaseResource(new ResourceCollection(staticResources.toArray))
      context.setWelcomeFiles(Array("index.html"))
      context.addServlet(classOf[DefaultServlet],"/*")
    }

    server.start
    server.join
  }
}
