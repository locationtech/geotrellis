package geotrellis.spark.io

import geotrellis.spark.LayerId
import scala.reflect._

/**
 * Configuration that allows you to specify rules for selecting target Params based on RasterRDD key type and LayerId.
 * LayerId defaults will be applies first, then the key defaults.
 *
 * ex: new DefaultParams[String]
 *     .withKeyParams[SpaceTimeKey]("rainforest_timelapse")
 *     .withKeyParams[SpatialKey]("satellite")
 *     .withLayerParams[SpatialKey]{
 *       case LayerId(name, zoom) if name.startsWith("NLCD") =>  s"satellite_nlcd_$zoom"
 *       case LayerId(name, zoom) if name.startsWith("CDC") =>  s"CDC_$zoom"
 *     }
 *
 */
class DefaultParams[Params](
  keyParams:   Map[ClassTag[_], Params] = Map.empty[ClassTag[_], Params],
  layerParams: Map[ClassTag[_], PartialFunction[LayerId, Params]] = Map.empty[ClassTag[_], PartialFunction[LayerId, Params]]) {

  /** Provide default Param based on type of layer key */
  def withKeyParams[K: ClassTag](param: Params) =
    new DefaultParams[Params](keyParams updated (classTag[K], param), layerParams)

  /** Provide default Params based on layer key and a partial function on LayerId */
  def withLayerParams[K: ClassTag](rule: PartialFunction[LayerId, Params]) =
    new DefaultParams[Params](keyParams, layerParams updated (classTag[K], rule))

  /** Discover best matching Params for a given K and LayerID */
  def paramsFor[K: ClassTag](id: LayerId): Option[Params] = {
    def default = keyParams.get(classTag[K])

    layerParams.get(classTag[K]) match {
      case Some(rule) => // look for LayerId based rule first
        if(rule isDefinedAt id ) Some(rule(id)) else default
      case None =>       // default to Key based rule
        default
    }
  }
}
