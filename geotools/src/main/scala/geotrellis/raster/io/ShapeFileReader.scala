/*
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
 */

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

  def readPoints[D](path:String,dataField:String):Seq[PointFeature[D]] = {
    // Convert to GeoTrellis features
    readSimpleFeatures(path) map { ft =>
      val geom = ft.getAttribute(0).asInstanceOf[jts.Point]
      val data = ft.getAttribute(dataField).asInstanceOf[D]
      PointFeature(geom,data)
    }
  }
}
