/*
 * Copyright 2018 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.layers.cog.vrt

import geotrellis.raster.{CellType, GridBounds, RasterExtent}
import geotrellis.layers.TileLayerMetadata
import geotrellis.layers.cog.vrt.VRT.{IndexedSimpleSource, SimpleSource, VRTRasterBand}
import geotrellis.vector.Extent
import geotrellis.proj4.CRS
import geotrellis.tiling._

import java.io.{BufferedWriter, ByteArrayOutputStream, OutputStreamWriter}

import scala.collection.JavaConverters._
import scala.xml.{Elem, XML}


case class VRT(
  gridBounds: GridBounds[Int],
  layout: LayoutDefinition,
  extent: Extent,
  cellType: CellType,
  crs: CRS,
  bands: List[Elem]
) {
  lazy val (layoutCols, layoutRows) = gridBounds.width * layout.tileCols -> gridBounds.height * layout.tileRows

  lazy val re: RasterExtent = RasterExtent(extent, layoutCols, layoutRows)

  /**
    * Calculates GeoTransform attributes
    *
    * TODO: refactor, code style kept to follow GDAL: https://github.com/OSGeo/gdal/blob/9a21e8dcaf36a7e046ee87cd57c8c03812dd20ed/gdal/frmts/sde/sdedataset.cpp
    */
  def geoTransform: (Double, Double, Double, Double, Double, Double) = {
    val origin = extent.center

    val x0 = origin.x
    val y0 = origin.y

    var dfMinX = extent.xmin
    var dfMinY = extent.ymin
    var dfMaxX = extent.xmax
    var dfMaxY = extent.ymax

    dfMaxX = (x0 - dfMinX) + dfMaxX
    dfMinY = (y0 - dfMaxY) + dfMinY

    // adjust the bbox based on the tile origin.
    dfMinX = math.min(x0, dfMinX)
    dfMaxY = math.max(y0, dfMaxY)

    if (dfMinX == 0.0 && dfMinY == 0.0 && dfMaxX == 0.0 && dfMaxY == 0.0)
      throw new Exception("Illegal raster extent")

    val rasterXSize = layoutCols
    val rasterYSize = layoutRows

    val padfTransform0 = dfMinX - 0.5 * (dfMaxX - dfMinX) / (rasterXSize - 1)
    val padfTransform3 = dfMaxY + 0.5 * (dfMaxY - dfMinY) / (rasterYSize - 1)

    val padfTransform1 = (dfMaxX - dfMinX) / (rasterXSize - 1)
    val padfTransform2 = 0d

    val padfTransform4 = 0d
    val padfTransform5 = -1 * (dfMaxY - dfMinY) / (rasterYSize - 1)

    (padfTransform0, padfTransform1, padfTransform2, padfTransform3, padfTransform4, padfTransform5)
  }

  /** GeoTransform attribtues as a string */
  def geoTransformString: String = {
    val (padfTransform0, padfTransform1, padfTransform2, padfTransform3, padfTransform4, padfTransform5) = geoTransform
    s"$padfTransform0, $padfTransform1, $padfTransform2, $padfTransform3, $padfTransform4, $padfTransform5"
  }

  /** Generates offsets of extent for the VRT doc */
  def extentToOffsets(extent: Extent): (Double, Double, Double, Double) = {
    val (xoff, yoff) = re.mapToGrid(extent.xmin, extent.ymax)
    val (xmax, ymin)  = re.mapToGrid(extent.xmax, extent.ymin)

    val xsize = xmax - xoff
    val ysize = ymin - yoff

    (xoff, yoff, xsize, ysize)
  }

  def cellTypeToString(ct: CellType): String =
    ct.getClass.getName.split("\\$").last.split("CellType").head.split("\\.").last.split("U").last

  /** Generates a tuple of a band and Elem, Elem contains a [[SimpleSource]] XML */
  def simpleSource(path: String, band: Int, xSize: Int, ySize: Int, extent: Extent): SimpleSource = {
    val (dstXOff, dstYOff, dstXSize, dstYSize) = extentToOffsets(extent)
    val elem =
      <SimpleSource>
        <SourceFilename relativeToVRT="1">{path}</SourceFilename>
        <SourceBand>{band.toString}</SourceBand>
        <SourceProperties RasterXSize={xSize.toString} RasterYSize={ySize.toString} DataType={cellTypeToString(cellType)} BlockXSize={layout.tileCols.toString} BlockYSize={layout.tileRows.toString}/>
        <SrcRect xOff="0" yOff="0" xSize={xSize.toString} ySize={ySize.toString}/>
        <DstRect xOff={dstXOff.toString} yOff={dstYOff.toString} xSize={dstXSize.toString} ySize={dstYSize.toString}/>
      </SimpleSource>

    band -> elem
  }

  /** Generates a list of [[VRTRasterBand]] (xml Elems) from a given list of [[SimpleSource]] */
  def simpleSourcesToBands(elems: List[SimpleSource]): List[VRTRasterBand] = {
    elems
      .groupBy(_._1)
      .toList
      .map { case (band, list) =>
        <VRTRasterBand dataType={cellTypeToString(cellType)} band={band.toString}>
          {list.map(_._2)}
        </VRTRasterBand>
      }
  }

  /** Creates a copy of a VRT object with [[SimpleSource]] elements as bands */
  def fromSimpleSources(elems: List[SimpleSource]): VRT =
    this.copy(bands = simpleSourcesToBands(elems))

  /** Represents a list of [[VRTRasterBand]] as a VRTDataset */
  def toXML(bands: List[VRTRasterBand]): Elem = {
    val rasterXSize = layoutCols
    val rasterYSize = layoutRows

    <VRTDataset rasterXSize={rasterXSize.toString} rasterYSize={rasterYSize.toString}>
      <SRS>{xml.Unparsed(crs.toWKT.get)}</SRS>
      <GeoTransform>{geoTransformString}</GeoTransform>
      {bands}
    </VRTDataset>
  }

  /** Represents a list of [[SimpleSource]] as a VRTDataset */
  def toXMLFromBands(elems: List[SimpleSource]): Elem =
    toXML(simpleSourcesToBands(elems))

  def write(path: String): Unit = XML.save(path, toXML(this.bands))

  def outputStream: ByteArrayOutputStream = VRT.outputStream(toXML(this.bands))
}

object VRT {
  /** SimpleSource is a tuple of band and SimpleSource [[Elem]] */
  type SimpleSource = (Int, Elem)
  /** A tuple of Index (long, typically and sfc file name) and a [[SimpleSource]] */
  type IndexedSimpleSource = (Long, SimpleSource)
  /** Alias for VRTRasterBand xml type, just a common [[Elem]] */
  type VRTRasterBand = Elem

  def outputStream(elem: Elem): ByteArrayOutputStream = {
    val baos = new ByteArrayOutputStream()
    val writer = new BufferedWriter(new OutputStreamWriter(baos))

    XML.write(
      w       = writer,
      node    = elem,
      enc     = XML.encoding,
      xmlDecl = false,
      doctype = null
    )

    baos
  }


  def apply[K: SpatialComponent](metadata: TileLayerMetadata[K]): VRT = {
    val gridBounds: GridBounds[Int] = metadata.bounds match {
      case kb: KeyBounds[K] => kb.toGridBounds()
      case EmptyBounds => throw new Exception("Empty iterator, can't generate a COG.")
    }

    VRT(gridBounds, metadata.layout, metadata.extent, metadata.cellType, metadata.crs, Nil)
  }

  def accumulatorName(layerName: String): String = s"vrt_samples_$layerName"
}
