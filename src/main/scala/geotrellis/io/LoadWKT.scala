package geotrellis.io

import com.vividsolutions.jts.io.WKTReader

import geotrellis._
import geotrellis.process._
import geotrellis.feature.Feature

//TODO: catch parse failure
case class LoadWkt(wkt:String) extends Op1(wkt) ({
  (wkt:String) => {
    val jtsGeom = new WKTReader().read(wkt)
    Result(Feature(jtsGeom,())) 
  }
})
