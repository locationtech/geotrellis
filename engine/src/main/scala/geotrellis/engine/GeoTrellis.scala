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

package geotrellis.engine

import geotrellis.engine._
import geotrellis.raster._

object GeoTrellis {
  private var _engine: Engine = null
  def engine = {
    if(!isInit) {
      init()
    }
    _engine
  }

  def isInit: Boolean = _engine != null

  def init(): Unit = init(GeoTrellisConfig())

  def init(config: GeoTrellisConfig, name: String = "geotrellis-engine"): Unit = {
    if(_engine!= null) {
      sys.error("GeoTrellis has already been initialized. You must only initiliaze once before shutdown.")
    }

    val catalog = config.catalogPath match {
      case Some(path) =>
        val file = new java.io.File(path)
        if(!file.exists()) {
          sys.error(s"Catalog path ${file.getAbsolutePath} does not exist. Please modify your settings.")
        }
        Catalog.fromPath(file.getAbsolutePath)
      case None => Catalog.empty(s"${name}-catalog")
    }
    _engine = Engine(name, catalog)
  }

  def run[T](op: Op[T]): OperationResult[T] = {
    engine.run(op)
  }

  def run[T](source: OpSource[T]): OperationResult[T] = {
    engine.run(source)
  }

  def get[T](op: Op[T]): T = {
    engine.get(op)
  }

  def get[T](source: OpSource[T]): T = {
    engine.get(source)
  }

  def shutdown() = {
    if(_engine != null) { 
      _engine.shutdown() 
      _engine = null
    }
  }
}
