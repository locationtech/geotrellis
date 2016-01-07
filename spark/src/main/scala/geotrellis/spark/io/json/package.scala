package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.io.index.hilbert.{HilbertSpaceTimeKeyIndex, HilbertSpatialKeyIndex}
import geotrellis.spark.io.index.rowmajor.RowMajorSpatialKeyIndex
import geotrellis.spark.io.index.KeyIndexIds
import geotrellis.spark.io.index.zcurve.{ZSpatialKeyIndex, ZSpaceTimeKeyIndex}
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.proj4.CRS
import geotrellis.raster._
import geotrellis.raster.io.json._
import geotrellis.vector._
import geotrellis.vector.io.json._

import com.github.nscala_time.time.Imports._
import org.apache.avro.Schema
import spray.json._
import spray.json.DefaultJsonProtocol._

package object json {
  // implicit def keyIndexFormat[K: ClassTag]: RootJsonFormat[index.KeyIndex[K]] =
  //   new JavaSerializationJsonFormat[index.KeyIndex[K]]

  /*implicit def keyIndexFormat[K: ClassTag] = new RootJsonFormat[index.KeyIndex[K]] {
    def write(obj: KeyIndex[K]): JsValue = {

      JsObject(
        "keyBounds" -> obj.keyBounds.toJson,
        "xResolution" -> obj.xResolution.toJson,
        "yResolution" -> obj.yResolution.toJson,
        "temporalResolution" -> obj.temporalResolution.toJson,
        "pattern" -> obj.pattern.toJson
      )
    }

    def read(value: JsValue): KeyIndex[K] = {
      value.asJsObject.getFields("keyBounds", "xResolution", "yResolution", "temporalResolution", "pattern") match {
        case Seq(keyBounds, xResolution, yResolution, temporalResolution, pattern) => {
          (keyBounds.convertTo[KeyBounds[K]],
           xResolution.convertTo[Int],
           yResolution.convertTo[Int],
           temporalResolution.convertTo[Int],
           pattern.convertTo[String]) match {
            case (kb, null, null, null, null) => RowMajorKeyIndexMethod.createIndex(kb.asInstanceOf[KeyBounds[SpatialKey]]).asInstanceOf[KeyIndex[K]]
            case (kb, xr, yr, null, null) => HilbertSpatialKeyIndex(kb.asInstanceOf[KeyBounds[SpatialKey]], xr, yr).asInstanceOf[KeyIndex[K]]
            case (kb, xr, yr, tr, null) => HilbertSpaceTimeKeyIndex(kb.asInstanceOf[KeyBounds[SpaceTimeKey]], xr, yr, tr).asInstanceOf[KeyIndex[K]]
            case (null, null, null, null, pattern) => ZCurveKeyIndexMethod.byPattern(pattern).createIndex(null.asInstanceOf[KeyBounds[SpaceTimeKey]]).asInstanceOf[KeyIndex[K]]
            case (null, null, null, null, null) => ZCurveKeyIndexMethod.createIndex(null.asInstanceOf[KeyBounds[SpatialKey]]).asInstanceOf[KeyIndex[K]]
          }
        }
        case _ =>
          throw new DeserializationException("err")
      }
    }
  }*/

  implicit object HilbertSpatialKeyIndexFormat extends RootJsonFormat[HilbertSpatialKeyIndex] {
    def write(obj: HilbertSpatialKeyIndex): JsValue = {
      JsObject(
        "id"   -> KeyIndexIds.hilbertSpatialKeyIndex.toJson,
        "args" -> JsObject(
          "keyBounds"          -> obj.keyBounds.toJson,
          "xResolution"        -> obj.xResolution.toJson,
          "yResolution"        -> obj.yResolution.toJson
        )
      )
    }

    def read(value: JsValue): HilbertSpatialKeyIndex = {
      value.asJsObject.getFields("id", "args") match {
        case Seq(JsString(id), args) => {
          if (id != KeyIndexIds.hilbertSpaceTimeKeyIndex) throw new DeserializationException("Wrong KeyIndex type.")
          args.convertTo[JsObject].getFields("keyBounds", "xResolution", "yResolution") match {
            case Seq(kb, xr, yr) =>
              HilbertSpatialKeyIndex(
                kb.convertTo[KeyBounds[SpatialKey]],
                xr.convertTo[Int],
                yr.convertTo[Int]
              )
            case _ =>
              throw new DeserializationException("Wrong KeyIndex constructor arguments.")
          }
        }
        case _ =>
          throw new DeserializationException("Wrong KeyIndex type.")
      }
    }
  }

  implicit object HilbertSpaceTimeKeyIndexFormat extends RootJsonFormat[HilbertSpaceTimeKeyIndex] {
    def write(obj: HilbertSpaceTimeKeyIndex): JsValue = {
      JsObject(
        "id"   -> KeyIndexIds.hilbertSpaceTimeKeyIndex.toJson,
        "args" -> JsObject(
          "keyBounds"          -> obj.keyBounds.toJson,
          "xResolution"        -> obj.xResolution.toJson,
          "yResolution"        -> obj.yResolution.toJson,
          "temporalResolution" -> obj.temporalResolution.toJson
        )
      )
    }

    def read(value: JsValue): HilbertSpaceTimeKeyIndex = {
      value.asJsObject.getFields("id", "args") match {
        case Seq(JsString(id), args) => {
          if (id != KeyIndexIds.hilbertSpaceTimeKeyIndex) throw new DeserializationException("Wrong KeyIndex type.")
          args.convertTo[JsObject].getFields("keyBounds", "xResolution", "yResolution", "temporalResolution") match {
            case Seq(kb, xr, yr, tr) =>
              HilbertSpaceTimeKeyIndex(
                kb.convertTo[KeyBounds[SpaceTimeKey]],
                xr.convertTo[Int],
                yr.convertTo[Int],
                tr.convertTo[Int]
              )
            case _ =>
              throw new DeserializationException("Wrong KeyIndex constructor arguments.")
          }
        }
        case _ =>
          throw new DeserializationException("Wrong KeyIndex type.")
      }
    }
  }

  implicit object RowMajorSpatialKeyIndexFormat extends RootJsonFormat[RowMajorSpatialKeyIndex] {
    def write(obj: RowMajorSpatialKeyIndex): JsValue = {
      JsObject(
        "id"   -> KeyIndexIds.rowMajorSpatialKeyIndex.toJson,
        "args" -> JsObject("keyBounds" -> obj.keyBounds.toJson)
      )
    }

    def read(value: JsValue): RowMajorSpatialKeyIndex = {
      value.asJsObject.getFields("id", "args") match {
        case Seq(JsString(id), args) => {
          if (id != KeyIndexIds.rowMajorSpatialKeyIndex) throw new DeserializationException("Wrong KeyIndex type.")
          args.convertTo[JsObject].getFields("keyBounds") match {
            case Seq(kb) => new RowMajorSpatialKeyIndex(kb.convertTo[KeyBounds[SpatialKey]])
            case _ =>
              throw new DeserializationException("Wrong KeyIndex constructor arguments.")
          }
        }
        case _ =>
          throw new DeserializationException("Wrong KeyIndex type.")
      }
    }
  }

  implicit object ZSpaceTimeKeyIndexFormat extends RootJsonFormat[ZSpaceTimeKeyIndex] {
    def write(obj: ZSpaceTimeKeyIndex): JsValue = {
      JsObject(
        "id"   -> KeyIndexIds.zSpaceTimeKeyIndex.toJson,
        "args" -> JsObject("pattern" -> obj.pattern.toJson)
      )
    }

    def read(value: JsValue): ZSpaceTimeKeyIndex = {
      value.asJsObject.getFields("id", "args") match {
        case Seq(JsString(id), args) => {
          if (id != KeyIndexIds.zSpaceTimeKeyIndex) throw new DeserializationException("Wrong KeyIndex type.")
          args.convertTo[JsObject].getFields("pattern") match {
            case Seq(JsString(p)) => ZSpaceTimeKeyIndex.byPattern(p)
            case _ =>
              throw new DeserializationException("Wrong KeyIndex constructor arguments.")
          }
        }
        case _ =>
          throw new DeserializationException("Wrong KeyIndex type.")
      }
    }
  }

  implicit object ZSpatialKeyIndexFormat extends RootJsonFormat[ZSpatialKeyIndex] {
    def write(obj: ZSpatialKeyIndex): JsValue = {
      JsObject(
        "id"   -> KeyIndexIds.zSpatialKeyIndex.toJson,
        "args" -> JsObject()
      )
    }

    def read(value: JsValue): ZSpatialKeyIndex = {
      value.asJsObject.getFields("id", "args") match {
        case Seq(JsString(id), args) => {
          if (id != KeyIndexIds.zSpaceTimeKeyIndex) throw new DeserializationException("Wrong KeyIndex type.")
          new ZSpatialKeyIndex()
        }
        case _ =>
          throw new DeserializationException("Wrong KeyIndex type.")
      }
    }
  }

  implicit object CRSFormat extends RootJsonFormat[CRS] {
    def write(crs: CRS) =
      JsString(crs.toProj4String)

    def read(value: JsValue): CRS = 
      value match {
        case JsString(proj4String) => CRS.fromString(proj4String)
        case _ => 
          throw new DeserializationException("CRS must be a proj4 string.")
      }
  }

  implicit object LayerIdFormat extends RootJsonFormat[LayerId] {
    def write(id: LayerId) =
      JsObject(
        "name" -> JsString(id.name),
        "zoom" -> JsNumber(id.zoom)
      )

    def read(value: JsValue): LayerId =
      value.asJsObject.getFields("name", "zoom") match {
        case Seq(JsString(name), JsNumber(zoom)) =>
          LayerId(name, zoom.toInt)
        case _ =>
          throw new DeserializationException("LayerId expected")
      }
  }

  implicit object LayoutDefinitionFormat extends RootJsonFormat[LayoutDefinition] {
    def write(obj: LayoutDefinition) =
      JsObject(
        "extent" -> obj.extent.toJson,
        "tileLayout" -> obj.tileLayout.toJson
      )

    def read(json: JsValue) =
      json.asJsObject.getFields("extent", "tileLayout") match {
        case Seq(extent, tileLayout) =>
          LayoutDefinition(extent.convertTo[Extent], tileLayout.convertTo[TileLayout])
        case _ =>
          throw new DeserializationException("LayoutDefinition expected")
      }
  }
  
  implicit object RasterMetaDataFormat extends RootJsonFormat[RasterMetaData] {
    def write(metaData: RasterMetaData) = 
      JsObject(
        "cellType" -> metaData.cellType.toJson,
        "extent" -> metaData.extent.toJson,
        "layoutDefinition" -> metaData.layout.toJson,
        "crs" -> metaData.crs.toJson
      )

    def read(value: JsValue): RasterMetaData =
      value.asJsObject.getFields("cellType", "extent", "layoutDefinition", "crs") match {
        case Seq(cellType, extent, layoutDefinition, crs) =>
          RasterMetaData(
            cellType.convertTo[CellType],
            layoutDefinition.convertTo[LayoutDefinition],
            extent.convertTo[Extent],
            crs.convertTo[CRS]
          )
        case _ =>
          throw new DeserializationException("RasterMetaData expected")
      }
  }

  implicit object RootDateTimeFormat extends RootJsonFormat[DateTime] {
    def write(dt: DateTime) = JsString(dt.withZone(DateTimeZone.UTC).toString)

    def read(value: JsValue) =
      value match {
        case JsString(dateStr) =>
          DateTime.parse(dateStr)
        case _ =>
          throw new DeserializationException("DateTime expected")
      }
  }

  implicit object SchemaFormat extends RootJsonFormat[Schema] {
    def read(json: JsValue) = (new Schema.Parser).parse(json.toString())
    def write(obj: Schema) = obj.toString.parseJson
  }
}
