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
