/***
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
 ***/

package geotrellis.process

/** A LayerId describes a layer in the catalog.
  * The data store is optional, but if there are more
  * than one layer with the same name in different datastores,
  * tyring to load that layer without specifying a datastore will
  * error.
  */
case class LayerId(store:Option[String],name:String)

object LayerId {
  /** Create a LayerId with no data store specified */
  def apply(name:String):LayerId = 
    LayerId(None,name)

  /** Create a LayerId with a data store specified */
  def apply(store:String,name:String):LayerId = 
    LayerId(Some(store),name)

  /** LayerId for in-memory rasters */
  val MEM_RASTER = LayerId(None, "mem-raster")
}
