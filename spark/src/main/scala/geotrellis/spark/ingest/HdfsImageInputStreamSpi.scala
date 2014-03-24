package geotrellis.spark.ingest
import org.apache.hadoop.fs.FSDataInputStream
import java.util.Locale
import scala.collection.JavaConversions._
import javax.imageio.spi.IIORegistry
import javax.imageio.spi.ImageInputStreamSpi
import javax.imageio.stream.ImageInputStream
import java.io.File
import org.apache.spark.Logging

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
  private var orderedProviders = false

  def orderInputStreamProviders: Unit = {
    // The input stream providers (SPIs) get created in no particular order. We
    // need to put our (geotrellis.spark) providers at the front of the list, so we can be sure they will
    // be created first, instead of one choosing one of the other types.

    if (!orderedProviders) {

      val registry = IIORegistry.getDefaultInstance()
      registry.registerServiceProvider(new HdfsImageInputStreamSpi, classOf[ImageInputStreamSpi])

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
  }
}