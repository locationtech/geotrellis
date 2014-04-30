package geotrellis.spark.ingest
import org.apache.hadoop.fs.FSDataInputStream
import org.apache.spark.Logging

import java.io.File
import java.util.Locale

import scala.collection.JavaConversions._

import javax.imageio.spi.IIORegistry
import javax.imageio.spi.ImageInputStreamSpi
import javax.imageio.stream.ImageInputStream

class HdfsImageInputStreamSpi extends ImageInputStreamSpi {

  override def canUseCacheFile() = false
  override def createInputStreamInstance(input: Object): ImageInputStream =
    createInputStreamInstance(input, false, null)

  override def createInputStreamInstance(input: Object, useCache: Boolean, cacheDir: File): ImageInputStream = {
    assert(input.isInstanceOf[FSDataInputStream])

    new HdfsImageInputStream(input.asInstanceOf[FSDataInputStream])
  }

  override def getDescription(locale: Locale) = "Service provider that instantiates a FSImageInputStream from a FSDataInputStream"
  override def getInputClass = classOf[FSDataInputStream]
  override def needsCacheFile = false

}

object HdfsImageInputStreamSpi extends Logging {

  /* 
   * This method does a blind deregisterAll whereas orderProviders below sorts our providers
   * above the system ones.  
   * 
   * The reason I favored an aggressive implementation is because orderProvider ends up adding   
   * our provider each time it is called. Over time, we accumulate too many of our providers.
   * This doesn't affect correctness but it is flaky to see so many of our instances. 
   * I tried calling IIORegistry.contains and also IIORegistry.deregisterProvider before 
   * calling IIORegistry.registerProvider but it doesn't work because the underlying implementation
   * checks for object equality using == and of course each time, we have a new object created there.
   * Singletons don't work either.   
   */    
  def register: Unit = {
    val registry = IIORegistry.getDefaultInstance()
    val start = System.currentTimeMillis()
    registry.deregisterAll()
    val end = System.currentTimeMillis()
    logInfo(s"Deregistering all providers in ${start - end} ms")
    val gtProvider = new HdfsImageInputStreamSpi
    val ret = registry.registerServiceProvider(gtProvider, classOf[ImageInputStreamSpi])
    logInfo(s"Registered HdfsImageInputStream SPI (${ret}) ")
  }

  /*private var orderedProviders = false

  def orderProviders: Unit = {
    // The input stream providers (SPIs) get created in no particular order. We
    // need to put our (geotrellis.spark) providers at the front of the list, so we can be sure they will
    // be created first, instead of one choosing one of the other types.

    if (!orderedProviders) {

      val registry = IIORegistry.getDefaultInstance()
      val gtProvider = new HdfsImageInputStreamSpi
      val ret = registry.registerServiceProvider(gtProvider, classOf[ImageInputStreamSpi])
      logInfo(s"Registered HdfsImageInputStream SPI (${ret}) ")

      val allProviders = registry.getServiceProviders(classOf[ImageInputStreamSpi], true).toArray
      val gtProviders = allProviders.filter(_.getClass().getCanonicalName().startsWith("geotrellis"))

      logInfo("printing all providers")
      allProviders.foreach(p => println(p.getClass().getCanonicalName()))
      logInfo("printing gt providers")
      allProviders.foreach(p => println(p.getClass().getCanonicalName()))

      for (
        nonGtProvider <- allProviders.filter(_.getClass().getCanonicalName().startsWith("geotrellis") != true)
      ) {
        gtProviders.foreach(gtProvider => {
          registry.setOrdering(classOf[ImageInputStreamSpi], gtProvider, nonGtProvider)
          logInfo(s"Ordering ${gtProvider.getClass().getCanonicalName()} over ${nonGtProvider.getClass().getCanonicalName()}")
        })

      }

      orderedProviders = true;

      println("Finished ordering")
    }
  }*/
}