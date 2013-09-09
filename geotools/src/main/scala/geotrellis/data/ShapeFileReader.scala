package geotrellis.data

import geotrellis._
import geotrellis.feature._
import geotrellis.raster.op._

import org.geotools.data.simple._
import org.opengis.feature.simple._
import org.geotools.data.shapefile._
import com.vividsolutions.jts.{geom => jts}

import java.net.URL
import java.io.File

import scala.collection.mutable

object ShapeFileReader {
  def readSimpleFeatures(path:String) = {
    // Extract the features as GeoTools 'SimpleFeatures'
    val url = s"file://${new File(path).getAbsolutePath}"
    val ftItr:SimpleFeatureIterator = 
      new ShapefileDataStore(new URL(url))
             .getFeatureSource()
             .getFeatures()
             .features()

    val simpleFeatures = mutable.ListBuffer[SimpleFeature]()
    while(ftItr.hasNext()) simpleFeatures += ftItr.next()
    simpleFeatures.toList
  }

  def readPoints[D](path:String,dataField:String):Seq[Point[D]] = {
    // Convert to GeoTrellis features
    readSimpleFeatures(path) map { ft =>
      val geom = ft.getAttribute(0).asInstanceOf[jts.Point]
      val data = ft.getAttribute(dataField).asInstanceOf[D]
      Point(geom,data)
    }
  }
}
