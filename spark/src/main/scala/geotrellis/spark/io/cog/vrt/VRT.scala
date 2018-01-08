package geotrellis.spark.io.cog.vrt

import geotrellis.raster.{CellType, GridBounds, RasterExtent}
import geotrellis.spark.io.cog.vrt.VRT.{IndexedSimpleSource, SimpleSource, VRTRasterBand}
import geotrellis.spark.{EmptyBounds, KeyBounds, SpatialComponent, TileLayerMetadata}
import geotrellis.vector.Extent

import org.apache.spark.util.CollectionAccumulator

import java.io.{BufferedWriter, ByteArrayOutputStream, OutputStreamWriter}

import scala.collection.JavaConverters._
import scala.xml.{Elem, XML}

case class VRT[K: SpatialComponent](base: TileLayerMetadata[K], bands: List[Elem] = Nil) {
  lazy val gb: GridBounds = base.bounds match {
    case kb: KeyBounds[K] => kb.toGridBounds()
    case EmptyBounds => throw new Exception("Empty iterator, can't generate a COG.")
  }

  lazy val (layoutCols, layoutRows) = gb.width * base.layout.tileCols -> gb.height * base.layout.tileRows

  lazy val re: RasterExtent = RasterExtent(base.extent, layoutCols, layoutRows)

  // TODO: refactor, code style kept to follow GDAL: https://github.com/OSGeo/gdal/blob/9a21e8dcaf36a7e046ee87cd57c8c03812dd20ed/gdal/frmts/sde/sdedataset.cpp
  def geoTransform: (Double, Double, Double, Double, Double, Double) = {
    val extent = base.extent
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

  def geoTransformString: String = {
    val (padfTransform0, padfTransform1, padfTransform2, padfTransform3, padfTransform4, padfTransform5) = geoTransform
    s"$padfTransform0, $padfTransform1, $padfTransform2, $padfTransform3, $padfTransform4, $padfTransform5"
  }

  def extentToOffsets(extent: Extent): (Double, Double, Double, Double) = {
    val (xoff, yoff) = re.mapToGrid(extent.xmin, extent.ymax)
    val (xmax, ymin)  = re.mapToGrid(extent.xmax, extent.ymin)

    val xsize = xmax - xoff
    val ysize = ymin - yoff

    (xoff, yoff, xsize, ysize)
  }

  def cellTypeToString(ct: CellType): String =
    ct.getClass.getName.split("\\$").last.split("CellType").head.split("\\.").last.split("U").last

  def simpleSource(path: String, band: Int, xSize: Int, ySize: Int, extent: Extent): SimpleSource = {
    val (dstXOff, dstYOff, dstXSize, dstYSize) = extentToOffsets(extent)
    val elem =
      <SimpleSource>
        <SourceFilename relativeToVRT="1">{path}</SourceFilename>
        <SourceBand>{band.toString}</SourceBand>
        <SourceProperties RasterXSize={xSize.toString} RasterYSize={ySize.toString} DataType={cellTypeToString(base.cellType)} BlockXSize={base.layout.tileCols.toString} BlockYSize={base.layout.tileRows.toString}/>
        <SrcRect xOff="0" yOff="0" xSize={xSize.toString} ySize={ySize.toString}/>
        <DstRect xOff={dstXOff.toString} yOff={dstYOff.toString} xSize={dstXSize.toString} ySize={dstYSize.toString}/>
      </SimpleSource>

    band -> elem
  }

  def simpleSourcesToBands(elems: List[SimpleSource]): List[VRTRasterBand] = {
    elems
      .groupBy(_._1)
      .toList
      .map { case (band, list) =>
        <VRTRasterBand dataType={cellTypeToString(base.cellType)} band={band.toString}>
          {list.map(_._2)}
        </VRTRasterBand>
      }
  }

  def fromSimpleSources(elems: List[SimpleSource]): VRT[K] =
    this.copy(bands = simpleSourcesToBands(elems))

  def fromAccumulator(acc: CollectionAccumulator[IndexedSimpleSource]): VRT[K] =
    fromSimpleSources(acc.value.asScala.toList.sortBy(_._1).map(_._2))

  def toXML(bands: List[VRTRasterBand]): Elem = {
    val rasterXSize = layoutCols
    val rasterYSize = layoutRows

    <VRTDataset rasterXSize={rasterXSize.toString} rasterYSize={rasterYSize.toString}>
      <SRS>{xml.Unparsed(base.crs.toWKT.get)}</SRS>
      <GeoTransform>{geoTransformString}</GeoTransform>
      {bands}
    </VRTDataset>
  }

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
}
