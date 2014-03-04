/*******************************************************************************
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
 ******************************************************************************/

package geotrellis.data.geojson

import geotrellis._
import geotrellis.feature._

import com.vividsolutions.jts.{ geom => jts }

object GeoJsonWriter {
  def removews(s:String) = """[\s]+""".r.replaceAllIn(s, m => "")

  def createFeatureCollectionString[T](geometry:Iterable[Geometry[T]],
                                       includeData:Boolean = true):String = {
    val features = 
      (for(g <- geometry) yield {
        createString(g,includeData)
      }).mkString(",")

    removews(
      s"""{
          "type": "FeatureCollection",
          "features": [ $features ]
          }"""
    )
  }

  def createString[T](geometry:Geometry[T],includeData:Boolean = true):String = {
    removews(geometry match {
      case p:Point[T] =>
        pointToString(p,includeData)
      case l:LineString[T] =>
        lineStringToString(l,includeData)
      case p:Polygon[T] =>
        polygonToString(p,includeData)
      case mp:MultiPoint[T] =>
        multiPointToString(mp,includeData)
      case ml:MultiLineString[T] =>
        multiLineStringToString(ml,includeData)
      case mp:MultiPolygon[T] =>
        multiPolygonToString(mp,includeData)
      case gc:GeometryCollection[T] =>
        geometryCollectionToString(gc,includeData)
      case _ =>
        sys.error("Unknown geometry")
    })
  }

  private def coordString(coords:Array[jts.Coordinate]) = {
    coords.map { c => s"[${c.x},${c.y}]" }
          .mkString(",")
  }

  private def polyString(p:jts.Polygon) = {
    val outerCoords = s"${coordString(p.getExteriorRing.getCoordinates)}"

    val innerCoords = 
      (for(i <- 0 until p.getNumInteriorRing) yield {
        s"${coordString(p.getInteriorRingN(i).getCoordinates)}"
      })
      .map { p => s"[$p]" }
      .toList

    (s"[$outerCoords]" :: innerCoords).mkString(",")
  }

  def jtsCreateString(g:jts.Geometry) = {
    g match {
      case p:jts.Point =>
        jtsPointToString(p)
      case l:jts.LineString =>
        jtsLineStringToString(l)
      case p:jts.Polygon =>
        jtsPolygonToString(p)
      case mp:jts.MultiPoint =>
        jtsMultiPointToString(mp)
      case ml:jts.MultiLineString =>
        jtsMultiLineStringToString(ml)
      case mp:jts.MultiPolygon =>
        jtsMultiPolygonToString(mp)
      case _ =>
        sys.error("Unknown JTS geometry")
    }
  }

  def jtsPointToString(p:jts.Point) = {
    s"""{
          "type": "Point",
          "coordinates": [${p.getX},${p.getY}] 
        }"""
  }

  def jtsLineStringToString(l:jts.LineString) = {
    s"""{
          "type": "LineString",
          "coordinates": [${coordString(l.getCoordinates)}] 
        }"""
  }

  def jtsPolygonToString(p:jts.Polygon) = {
    s"""{
          "type": "Polygon",
          "coordinates": [${polyString(p)}] 
         }"""
  }

  def jtsMultiPointToString(mp:jts.MultiPoint) = {
    s"""{
          "type": "MultiPoint",
          "coordinates": [${coordString(mp.getCoordinates)}] 
        }"""
  }

  def jtsMultiLineStringToString(ml:jts.MultiLineString) = {
    val coords = 
      (for(i <- 0 until ml.getNumGeometries) yield {
        s"[${coordString(ml.getGeometryN(i).getCoordinates)}]"
      }).toList
       .mkString(",")

    s"""{
          "type": "MultiLineString",
          "coordinates": [$coords] 
        }"""
  }

  def jtsMultiPolygonToString(mp:jts.MultiPolygon) = {
    val coords = 
      (for(i <- 0 until mp.getNumGeometries) yield {
        s"[${polyString(mp.getGeometryN(i).asInstanceOf[jts.Polygon])}]"
      }).toList
       .mkString(",")

    s"""{
          "type": "MultiPolygon",
          "coordinates": [${coords}] 
        }"""
  }

  def pointToString[T](p:Point[T],includeData:Boolean = true) = {
    val props =
      if(includeData) {
        s""" ,"properties": { "data": "${p.data}" } """
      } else {
        ""
      }

    s""" { "type": "Feature",
           "geometry": ${jtsPointToString(p.geom)}
           $props
         }"""
  }

  def lineStringToString[T](l:LineString[T],includeData:Boolean = true) = {
    val props =
      if(includeData) {
        s""" ,"properties": { "data": "${l.data}" } """
      } else {
        ""
      }

    s""" { "type": "Feature",
           "geometry": ${jtsLineStringToString(l.geom)}
           $props
         }"""
  }

  def polygonToString[T](p:Polygon[T],includeData:Boolean = true) = {
    val props =
      if(includeData) {
        s""" ,"properties": { "data": "${p.data}" } """
      } else {
        ""
      }

    s""" { "type": "Feature",
           "geometry": ${jtsPolygonToString(p.geom)}
           $props
         }"""
  }

  def multiPointToString[T](mp:MultiPoint[T],includeData:Boolean = true) = {
    val props =
      if(includeData) {
        s""" ,"properties": { "data": "${mp.data}" } """
      } else {
        ""
      }

    s""" { "type": "Feature",
           "geometry": ${jtsMultiPointToString(mp.geom)}
           $props
         }"""
  }

  def multiLineStringToString[T](ml:MultiLineString[T],includeData:Boolean = true) = {
    val props =
      if(includeData) {
        s""" ,"properties": { "data": "${ml.data}" } """
      } else {
        ""
      }

    s""" { "type": "Feature",
           "geometry": ${jtsMultiLineStringToString(ml.geom)}
           $props
         }"""
  }

  def multiPolygonToString[T](mp:MultiPolygon[T],includeData:Boolean = true) = {
    val props =
      if(includeData) {
        s""" ,"properties": { "data": "${mp.data}" } """
      } else {
        ""
      }

    s""" { "type": "Feature",
           "geometry": ${jtsMultiPolygonToString(mp.geom)}
           $props
         }"""
  }

  def geometryCollectionToString[T](gc:GeometryCollection[T],includeData:Boolean = true) = {
    val geoms =
      (for(i <- 0 until gc.geom.getNumGeometries) yield {
        s"${jtsCreateString(gc.geom.getGeometryN(i))}"
      }).toList
       .mkString(",")

    val props =
      if(includeData) {
        s""" ,"properties": { "data": "${gc.data}" } """
      } else {
        ""
      }

    s""" { "type": "Feature",
           "geometry": {
              "type": "GeometryCollection",
              "geometries": [ $geoms ]
           }
           $props
         }"""
  }
}
