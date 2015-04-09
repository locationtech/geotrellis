package geotrellis.spark

import org.scalatest._
import org.scalatest.BeforeAndAfterAll
import org.gdal.gdal.gdal

trait OnlyIfGdalInstalled extends FunSpec with BeforeAndAfterAll {

  def ifGdalInstalled(f: => Unit): Unit = {
    try {
      gdal.AllRegister()
      f
    } catch {
      case e: java.lang.UnsatisfiedLinkError => ignore("Java GDAL bindings not installed, skipping"){}
    }
  }
}
