from geotrellis.spark.KeyBounds import Bounds, EmptyBounds, KeyBounds
from geotrellis.spark.SpatialKey import SpatialKey
from geotrellis.raster.CellSize import CellSize
from geotrellis.vector.Extent import Extent, ProjectedExtent
from geotrellis.python.crs.CRSWrapper import CRS
from geotrellis.spark.tiling.LayoutDefinition import LayoutDefinition

class TileLayerMetadata(object):
    def __init__(self, cellType, layout, extent, crs, bounds):
        self.cellType = cellType
        self.layout = layout
        self.extent = extent
        self.crs = crs
        self.bounds = bounds

    def __eq__(self, other):
        if not isinstance(other, TileLayerMetadata):
            return False
        return (self.cellType == other.cellType and
                self.layout == other.layout and
                self.extent == other.extent and
                self.crs == other.crs and
                self.bounds == other.bounds)

    def __hash__(self):
        return hash((self.cellType, self.layout, self.extent, self.crs, self.bounds))

    def _copy(self, cellType = None, layout = None, extent = None, crs = None, bounds = None):
        cellType = cellType if cellType is not None else self.cellType
        layout = layout if layout is not None else self.layout
        extent = extent if extent is not None else self.extent
        crs = crs if crs is not None else self.crs
        bounds = bounds if bounds is not None else self.bounds
        return TileLayerMetadata(cellType, layout, extent, crs, bounds)

    @property
    def mapTransform(self):
        return self.layout.mapTransform

    @property
    def tileLayout(self):
        return self.layout.tileLayout

    @property
    def layoutExtent(self):
        return self.layout.extent

    @property
    def gridBounds(self):
        return self.mapTransform(self.extent)

    def combine(self, other):
        combinedExtent = self.extent.combine(other.extent)
        combinedLayoutExtent = self.layoutExtent.combine(other.layoutExtent)
        combinedTileLayout = self.tileLayout.combine(other.tileLayout)
        newLayout = LayoutDefinition(
                combinedLayoutExtent,
                combinedTileLayout)

        return self._copy(
                extent = combinedExtent,
                layout = newLayout)

    def updateBounds(self, newBounds):
        if newBounds is EmptyBounds:
            newExtent = Extent(
                    self.extent.xmin, self.extent.ymin,
                    self.extent.xmin, self.extent.ymin)
            return self._copy(
                    extent = newExtent,
                    bounds = newBounds)
        kbExtent = self.mapTransform(newBounds.toGridBounds)
        intersection = kbExtent.intersection(self.extent)
        if intersection is not None:
            return self._copy(bounds = newBounds, extent = intersection)
        else:
            newExtent = Extent(
                    self.extent.xmin, self.extent.ymin,
                    self.extent.xmin, self.extent.ymin)
            return self._copy(bounds = newBounds, extent = newExtent)

    def getComponent(self, componentType):
        if componentType == Extent:
            return self.extent
        elif componentType == CRS:
            return self.crs
        elif componentType == LayoutDefinition:
            return self.layout
        elif componentType == Bounds:
            return self.bounds
        else:
            raise Exception("wrong componentType: {0}".format(componentType))

    def setComponent(self, value):
        if isinstance(value, LayoutDefinition):
            return self._copy(layout = value)
        elif isinstance(value, Bounds):
            return self._copy(bounds = value)
        else:
            raise Exception("wrong argument: {0}".format(value))

    @staticmethod
    def fromRdd(rdd, first, second = None):
        if (isinstance(first, CRS) and
                isinstance(second, LayoutDefinition)):
            crs, layout = first, second
            extent, cellType, _cellSize, bounds = _collectMetadata(rdd)
            kb = bounds.setSpatialBounds(KeyBounds(layout.mapTransform(extent)))
            return TileLayerMetadata(cellType, layout, extent, crs, kb)
        elif isinstance(first, LayoutDefinition) and second is None:
            layout = first
            extent, cellType, _cellSize, bounds, crs = _collectMetadataWithCRS(rdd)
            kb = bounds.setSpatialBounds(KeyBounds(layout.mapTransform(extent)))
            return TileLayerMetadata(cellType, layout, extent, crs, kb)
        else:
            # TODO implement LayoutScheme case
            raise Exception("Not implemented")


def _collectMetadata(rdd):
    def mapper(t):
        key, grid = t
        extent = key.extent
        boundsKey = key.translate(SpatialKey(0,0))
        return (extent,
                grid.cellType,
                CellSize(extent, grid.cols, grid.rows)
                KeyBounds(boundsKey, boundsKey))
    def reducer(tuple1, tuple2):
        extent1, cellType1, cellSize1, bounds1 = tuple1
        extent2, cellType2, cellSize2, bounds2 = tuple2
        return (extent1.combine(extent2),
                cellType1.union(cellType2),
                cellSize1 if cellSize1.resolution < cellSize2.resolution else cellSize2,
                bounds1.combine(bounds2))
    return rdd.map(mapper).reduce(reducer)

def _collectMetadataWithCRS(rdd):
    def mapper(t):
        key, grid = t
        projectedExtent = key.getComponent(ProjectedExtent)
        extent = projectedExtent.extent
        crs = projectedExtent.crs
        boundsKey = key.translate(SpatialKey(0,0))
        return (extent,
                grid.cellType,
                CellSize(extent, grid.cols, grid.rows),
                set(crs),
                KeyBounds(boundsKey, boundsKey))
    def reducer(tuple1, tuple2):
        extent1, cellType1, cellSize1, crs1, bounds1 = tuple1
        extent2, cellType2, cellSize2, crs2, bounds2 = tuple2
        return (extent1.combine(extent2),
                cellType1.union(cellType2),
                cellSize1 if cellSize1.resolution < cellSize2.resolution else cellSize2,
                crs1.union(crs2),
                bounds1.combine(bounds2))
    extent, cellType, cellSize, crsSet, bounds = rdd.map(mapper).reduce(reducer)
    if len(crsSet) != 1:
        raise Exception("Multiple CRS tags found: {0}".format(crsSet))
    return (extent, cellType, cellSize, bounds, crsSet[0])
