package geotrellis.rest

import com.typesafe.config.{ConfigFactory,Config}

import scala.collection.mutable

case class ServerConfig(packages:Seq[String],
                        jetty:JettyConfig,
                        geotrellis:GeoTrellisConfig,
                        admin:AdminConfig,
                        staticContentPath:Option[String])

object ServerConfig {
  def init():ServerConfig = init(ConfigFactory.load())
  def init(config:Config):ServerConfig = {
    val packages = mutable.Set[String]()
  
    if(config.hasPath("geotrellis.rest-package")) {
      packages += config.getString("geotrellis.rest-package")
    }

    val staticContentPath = if(config.hasPath("geotrellis.server.serve-static")) {
      if(config.getBoolean("geotrellis.server.serve-static")) {
        Some(config.getString("geotrellis.server.static-path"))
      } else { None }
    } else { None }

    val jetty = JettyConfig.init(config)
    val geotrellis = GeoTrellisConfig.init(config)
    val admin = AdminConfig.init(config)

    if(admin.serveSite) {
      packages += "geotrellis.admin.services"
    }

    ServerConfig(packages.toSeq,
                 jetty,
                 geotrellis,
                 admin,
                 staticContentPath)
  }
}

case class JettyConfig(host:String,
                       port:Int,
                       corePoolSize:Int,
                       maximumPoolSize:Int,
                       keepAliveTime:Long,
                       serveStatic:Boolean,
                       contextPath:String)
object JettyConfig {
  def init():JettyConfig = init(ConfigFactory.load())
  def init(config:Config):JettyConfig = {
    val host = config.getString("geotrellis.host")
    val port = config.getInt("geotrellis.port")

    val corePoolSize = config.getInt("geotrellis.jetty.corePoolSize")
    val maximumPoolSize = config.getInt("geotrellis.jetty.maximumPoolSize")
    val keepAliveTime = config.getLong("geotrellis.jetty.keepAliveMilliseconds")

    val serveStatic = config.hasPath("geotrellis.server.serve-static") &&
                      config.getBoolean("geotrellis.server.serve-static")

    val contextPath = 
      if(config.hasPath("geotrellis.rest-prefix")) {
        val s = config.getString("geotrellis.rest-prefix") + "/*"
        if(!s.startsWith("/")) { s"/$s" }
        else { s }
      } else {
        "/gt/*"
      }

    JettyConfig(host,port,corePoolSize,maximumPoolSize,keepAliveTime,serveStatic,contextPath)
  }
}

case class GeoTrellisConfig(catalogPath:Option[String])
object GeoTrellisConfig {
  def init():GeoTrellisConfig = init(ConfigFactory.load())
  def init(config:Config):GeoTrellisConfig = {
    val catalogPath = if(config.hasPath("geotrellis.catalog")) {
      Some(config.getString("geotrellis.catalog"))
    } else { None }
    GeoTrellisConfig(catalogPath)
  }
}

case class AdminConfig(serveSite:Boolean,serveFromJar:Boolean)
object AdminConfig {
  def init():AdminConfig = init(ConfigFactory.load())
  def init(config:Config):AdminConfig = {    
    val serveSite = config.hasPath("geotrellis.admin.serve-site") &&
                    config.getBoolean("geotrellis.admin.serve-site")

    val serveFromJar = config.hasPath("geotrellis.admin.serve-from-jar") &&
                       config.getBoolean("geotrellis.admin.serve-from-jar")

    AdminConfig(serveSite,serveFromJar)
  }
}
