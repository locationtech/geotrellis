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

package geotrellis.process

import scala.collection.mutable
import java.io.File
import geotrellis._
import geotrellis.process._
import geotrellis.util._
import geotrellis.util.Filesystem
import geotrellis.data.AsciiRasterLayerBuilder

import com.typesafe.config.Config

import java.io.File
import scala.util._

// example json is available in the geotrellis.process.catalog tests. please
// keep it up-to-date with changes you make here.

/**
 * Represents a named collection of data stores. We expect each JSON file to
 * correspond to one catalog.
 */
case class Catalog(name:String, stores:Map[String, DataStore], json: String,source: String) {

  private var cacheSet = false
  def initCache(cache:Option[Cache[String]]):Unit =
    if(!cacheSet) {
      // Set the cache on all layers.
      for(store <- stores.values) { store.setCache(cache) }

      // Cache all layers if the DataStore has cacheAll set
      stores.values
            .filter(_.hasCacheAll)
            .map(_.cacheAll)

      // If the DataStore didn't cache due to the cacheAll flag,
      // find any layer that has the cache flag set and cache those layers.
      stores.values
            .filter(!_.hasCacheAll)
            .map(_.getLayers)
            .flatten
            .filter(_.info.shouldCache)
            .map(_.cache)

      cacheSet = true
    } else {
      sys.error("Cache has already been set for this Catalog. " +
                "You may not set the cache more than once during a Catalog's lifetime.")
    }

  def initCache(cache:Cache[String]):Unit = initCache(Some(cache))

  def getRasterLayer(layerId:LayerId):Try[RasterLayer] =
    layerId.store match {
      case Some(ds) => 
        stores.get(ds) match {
          case Some(store) =>
            store.getRasterLayer(layerId.name) match {
              case Some(layer) => Success(layer)
              case None => Failure(new java.io.IOException(s"No raster with name ${layerId.name} exists in store ${ds}"))
            }
          case None => Failure(new java.io.IOException(s"No store with name $ds exists in the catalog."))
        }
      case None => 
        stores.values.flatMap(_.getRasterLayer(layerId.name)).toList match {
          case Nil => Failure(new java.io.IOException(s"No raster with name ${layerId.name} exists in the catalog."))
          case layer :: Nil => Success(layer)
          case _ =>
            Failure(new java.io.IOException(s"There are multiple layers named '${layerId.name}' in the catalog. You must specify a datastore"))
        }
    }

  def layerExists(layerId:LayerId):Boolean =
    layerId.store match {
      case Some(ds) =>
        stores.get(ds) match {
          case Some(store) =>
            store.getRasterLayer(layerId.name) match {
              case Some(layer) => true
              case None => false
            }
          case None => false
        }
      case None =>
        stores.values.flatMap(_.getRasterLayer(layerId.name)).toList match {
          case Nil => false
          case layer :: Nil => true
          case _ => true
        }
    }

  def getStore(name:String) = stores.get(name)
}

object Catalog {
  private val stringToRasterLayerBuilder = 
    mutable.Map[String,RasterLayerBuilder](
      "constant" -> ConstantRasterLayerBuilder,
      "ascii" -> AsciiRasterLayerBuilder,
      "arg" -> ArgFileRasterLayerBuilder,
      "tiled" -> TileSetRasterLayerBuilder,
      "argurl" -> ArgUrlRasterLayerBuilder
    )

  def addRasterLayerBuilder(layerType:String,builder:RasterLayerBuilder) =
    if(stringToRasterLayerBuilder.contains(layerType)) {
      println(s"WARNING: A raster layer builder is already registered for the layer type '$layerType'")
    } else {
      stringToRasterLayerBuilder(layerType) = builder
    }

  def getRasterLayerBuilder(layerType:String):Option[RasterLayerBuilder] = 
    if(stringToRasterLayerBuilder.contains(layerType)) {
      Some(stringToRasterLayerBuilder(layerType))
    } else {
      None
    }

  /**
   * Build a Catalog instance given a path to a JSON file.
   */
  def fromPath(path:String): Catalog =
    fromJSON(Filesystem.readText(path), path)

  def fromJSON(data:String):Catalog = fromJSON(data,"")

  /**
   * Build a Catalog instance given a string of JSON data.
   */
  def fromJSON(data:String,path:String): Catalog = 
    json.CatalogParser(data,path).create(data,path)

  /**
   * Builds an empty Catalog.
   */
  def empty(name:String) = Catalog(name, Map.empty[String, DataStore], "{}","empty()")
}
