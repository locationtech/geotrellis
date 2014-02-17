package geotrellis.spark
import geotrellis.spark.utils.SparkUtils

import org.apache.log4j.Level
import org.apache.log4j.Logger

import org.apache.spark.SparkContext
import org.scalatest.BeforeAndAfter
import org.scalatest.FunSpec

object SparkTest extends org.scalatest.Tag("geotrellis.spark.test.tags.SparkTest")

trait SparkEnvironment extends FunSpec with BeforeAndAfter {
  var sc: SparkContext = _

  def sparkTest(name: String, silenceSpark: Boolean = true)(body: => Unit) {
    describe(name) {
      val origLogLevels = if (silenceSpark) SparkLogging.silence() else Level.INFO
      sc = SparkUtils.createSparkContext("local", name)
      try {
        body
      } finally {
        sc.stop
        sc = null
        // To avoid Akka rebinding to the same port, since it doesn't unbind immediately on shutdown
        System.clearProperty("spark.master.port")
        if (silenceSpark) SparkLogging.setLogLevels(origLogLevels, SparkLogging.components)
      }
    }

  }
}

object SparkLogging {
  val components = Seq("spark", "org.eclipse.jetty", "akka")
  def silence(): Level = {
    setLogLevels(Level.WARN, components)
    Level.INFO
  }

  def setLogLevels(level: org.apache.log4j.Level, loggers: TraversableOnce[String]) = {
    loggers.map {
      loggerName =>
        val logger = Logger.getLogger(loggerName)
        val prevLevel = logger.getLevel()
        logger.setLevel(level)
        loggerName -> prevLevel
    }.toMap
  }

}