from geotrellis.spark.io.avro.AvroRecordCodec import AvroRecordCodec
from edited_avro.avro_builder import AvroSchemaBuilder
from geotrellis.raster.CellType import (
        ShortConstantNoDataCellType, ShortCellType, ShortUserDefinedNoDataCellType,
        UShortConstantNoDataCellType, UShortCellType, UShortUserDefinedNoDataCellType
        )
from geotrellis.raster.ShortArrayTile import ShortArrayTile
from geotrellis.raster.UShortArrayTile import UShortArrayTile
from geotrellis.raster.package_scala import SHORTMIN, isNoData

import array

class ShortArrayTileCodec(AvroRecordCodec):
    @property
    def schema(self):
        _ = AvroSchemaBuilder()
        _.begin_record("ShortArrayTile", namespace = "geotrellis.raster")
        _.add_field("cols", _.create_int())
        _.add_field("rows", _.create_int())
        _.add_field("cells",
                _.begin_array(_.create_int()).end())
        _.add_field("noDataValue",
                _.begin_union(
                    _.create_int(),
                    _.create_null()
                    ).end())
        return _.end()

    def _encode(self, tile, dct):
        dct["cols"] = tile.cols
        dct["rows"] = tile.rows
        dct["cells"] = tile.cells
        if tile.cellType is ShortConstantNoDataCellType:
            dct["noDataValue"] = SHORTMIN
        elif tile.cellType is ShortCellType:
            dct["noDataValue"] = None # TODO find matching java's null alternative
        elif isinstance(tile.cellType, ShortUserDefinedNoDataCellType):
            dct["noDataValue"] = tile.cellType.noDataValue
        else:
            raise Exception(
                    "CellType {ct} was unexpected".format(ct = tile.cellType))

    def decode(self, dct):
        arr = map(int, dct["cells"])
        arr = array.array('h', arr)
        if "noDataValue" not in dct.keys():
            cellType = ShortCellType
        else:
            nodata = int(dct["noDataValue"])
            if nodata == SHORTMIN:
                cellType = ShortConstantNoDataCellType
            else:
                cellType = ShortUserDefinedNoDataCellType(nodata)

        cols = int(dct["cols"])
        rows = int(dct["rows"])
        return ShortArrayTile.applyStatic(arr, cols, rows, cellType)

class UShortArrayTileCodec(AvroRecordCodec):
    @property
    def schema(self):
        _ = AvroSchemaBuilder()
        _.begin_record("UShortArrayTile", namespace = "geotrellis.raster")
        _.add_field("cols", _.create_int())
        _.add_field("rows", _.create_int())
        _.add_field("cells",
                _.begin_array(_.create_int()).end())
        _.add_field("noDataValue",
                _.begin_union(
                    _.create_int(),
                    _.create_null()
                    ).end())
        return _.end()

    def _encode(self, tile, dct):
        dct["cols"] = tile.cols
        dct["rows"] = tile.rows
        dct["cells"] = tile.cells
        if tile.cellType is UShortConstantNoDataCellType:
            dct["noDataValue"] = 0
        elif tile.cellType is UShortCellType:
            dct["noDataValue"] = None
        elif isinstance(tile.cellType, UShortUserDefinedNoDataCellType):
            dct["noDataValue"] = tile.cellType.noDataValue
        else:
            raise Exception(
                    "CellType {ct} was unexpected".format(ct = tile.cellType))

    def decode(self, dct):
        arr = map(int, dct["cells"])
        arr = array.array('H', arr)
        if "noDataValue" not in dct.keys():
            cellType = UShortCellType
        else:
            nodata = int(dct["noDataValue"])
            if nodata == 0:
                cellType = UShortConstantNoDataCellType
            else:
                cellType = UShortUserDefinedNoDataCellType(nodata)

        cols = int(dct["cols"])
        rows = int(dct["rows"])
        return UShortArrayTile.applyStatic(arr, cols, rows, cellType)

class IntArrayTileCodec(AvroRecordCodec):
    @property
    def schema(self):
        _ = AvroSchemaBuilder()
        _.begin_record("IntArrayTile", namespace = "geotrellis.raster")
        _.add_field("cols", _.create_int())
        _.add_field("rows", _.create_int())
        _.add_field("cells",
                _.begin_array(_.create_int()).end())
        _.add_field("noDataValue",
                _.begin_union(
                    _.create_int(),
                    _.create_null()
                    ).end())
        return _.end()

    def _encode(self, tile, dct):
        dct["cols"] = tile.cols
        dct["rows"] = tile.rows
        dct["cells"] = tile.cells
        if tile.cellType is IntConstantNoDataCellType:
            dct["noDataValue"] = INTMIN
        elif tile.cellType is IntCellType:
            dct["noDataValue"] = None
        elif isinstance(tile.cellType, IntUserDefinedNoDataCellType):
            dct["noDataValue"] = tile.cellType.noDataValue
        else:
            raise Exception(
                    "CellType {ct} was unexpected".format(ct = tile.cellType))

    def decode(self, dct):
        arr = map(int, dct["cells"])
        arr = array.array('i', arr)
        if "noDataValue" not in dct.keys():
            cellType = IntCellType
        else:
            nodata = int(dct["noDataValue"])
            if isNoData(nodata):
                cellType = IntConstantNoDataCellType
            else:
                cellType = IntUserDefinedNoDataCellType(nodata)

        cols = int(dct["cols"])
        rows = int(dct["rows"])
        return IntArrayTile.applyStatic(arr, cols, rows, cellType)
