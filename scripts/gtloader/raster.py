"""
Defines classes that represent Rasters.
"""
import os, sys
import struct
import array
import json
import gdal
import math

import log
import projection
from arg import ArgWriter
from datatypes import *

class Extent():
    def __init__(self, xmin, ymin, xmax, ymax):
        self.xmin = xmin
        self.ymin = ymin
        self.xmax = xmax
        self.ymax = ymax

    def __str__(self):
        return "Extent(%f,%f,%f,%f)" % (self.xmin,self.ymin,self.xmax,self.ymax)

class RasterExtent():
    def __init__(self, extent, cellwidth, cellheight, cols, rows):
        self.extent = extent
        self.cellwidth = cellwidth
        self.cellheight = cellheight
        self.cols = cols
        self.rows = rows

class GdalLayer():
    """
    Represents one band in a raster as read by GDAL.
    """
    def __init__(self, path, band = 1):
        self.path = path
        self.dataset = gdal.Open(path, GA_ReadOnly)

        bands = self.dataset.RasterCount
        if bands < band:
            log.error("This raster does not contain a band number %d" % band)
        self.band = self.dataset.GetRasterBand(band)

        self.epsg = projection.get_epsg(self.dataset)
        xmin,xres,rot1,ymin,rot2,yres = self.dataset.GetGeoTransform()
        self.rot1 = rot1
        self.rot2 = rot2
        
        cols = self.dataset.RasterXSize
        rows = self.dataset.RasterYSize

        xmax = xmin + cols*xres
        ymax = ymin + rows*yres

        # Since xres and yres can be negative,
        # we simply use min/max to select the proper bounding
        # box
        extent = Extent(min(xmin,xmax),
                        min(ymin,ymax),
                        max(xmin,xmax),
                        max(ymin,ymax))

        self.raster_extent = RasterExtent(extent,abs(xres),abs(yres),cols,rows)

        self.datatype = to_datatype_arg(self.band.DataType)
        self.fchar = to_datatype_str(self.band.DataType)

    def toMap(self):
        extent = self.raster_extent.extent
        re = self.raster_extent
        return {
            'type': 'arg',
            'datatype': self.datatype,
            'xmin': extent.xmin,
            'ymin': extent.ymin,
            'xmax': extent.xmax,
            'ymax': extent.ymax,
            'cellwidth': re.cellwidth,
            'cellheight': re.cellheight,
            'rows': re.rows,
            'cols': re.cols,
            'xskew': 0,
            'yskew': 0,
            'epsg': self.epsg
        }

    def read_row(self,row,offset=0,size=None):
        """
        Reads a row from this raster.
        Optionally you can specify an offset and size
        to read only a section of the row.
        """
        # Network Byte Order (Big Endian)
        if size is None:
            size = self.cols

        unpack_str = '%d%s' % (size,self.fchar)

        scanline = self.band.ReadRaster(
            offset, row, size, 1, size, 1, self.band.DataType)

        return list(struct.unpack(unpack_str, scanline))

    def write_metadata(self, path, name, data_type = None):
        m = self.toMap()
        m['layer'] = name
        if data_type:
            m['datatype'] = data_type

        with file(path,'w') as mdf:
            mdf.write(
                json.dumps(m,
                           sort_keys=True,
                           indent=4,
                           separators=(',',': ')) + '\n')

    def write_arg(self, 
                  path, 
                  data_type = None, 
                  window = None,
                  printprg = True,
                  verify = True):
        """
        Writes this layer out as an ARG.

        Parameters:
          path: Path to the arg file to be generated.
          data_type: Data type of the output arg.
          window: 4-tuple of (col_min, row_min, col_max, row_max) 
                  of the window of the raster to be exported.
                  Defaults to the entire raster.
          verify: Set to false to not verify data type conversion (just truncate)
          printprg: Set to false to suppress printing progress.
        """
        if not data_type:
            data_type = self.datatype
            
        # Process the windows for clipping
        if window is None:
            window = (0, 0, self.raster_extent.cols, self.raster_extent.rows)

        if len(window) != 4:
            log.error('Invalid window: %s' % window)

        start_col, end_col = window[0], window[2]
        total_cols =  end_col - start_col

        start_row, end_row = window[1], window[3]
        total_rows = end_row - start_row

        ndv = self.band.GetNoDataValue()
        # if NoData is 128 and type is byte, 128 is read in as -128
        if self.datatype == 'int8' and ndv == 128:
            ndv = -128

        arg_no_data = nodata_for_fmt(data_type)

        psize = int(total_rows / 100)

        output = file(path, 'wb')
        writer = ArgWriter(output,data_type,verify)

        # If needed, add NoData values to the start or end of the row
        prerow = [arg_no_data] * (0 - start_col)
        start_col = max(0,start_col)
        postrow = [arg_no_data] * (end_col - self.raster_extent.cols)
        total_cols_to_scan = min(end_col, self.raster_extent.cols) - start_col

        for row in xrange(start_row,end_row):
            if printprg and psize != 0 and row % psize == 0:
                row_progress = row - start_row
                sys.stdout.write('%d/%d (%d%%) completed\r' % (row_progress,
                                                               total_rows,
                                                               row_progress*100/total_rows))
                sys.stdout.flush()
                output.flush()

            if row < 0 or row >= self.raster_extent.rows:
                ar = [arg_no_data] * total_cols
            else:
                ar = self.read_row(row,start_col,total_cols_to_scan)
                if prerow:
                    ar = prerow + ar
                if postrow:
                    ar = ar + postrow
            # Replace nodata before verification
            data = array.array(to_struct_fmt(data_type), ar)
            for i in xrange(0,len(data)):
                if data[i] == ndv:
                    data[i] = arg_no_data

            writer.write(data)

        output.flush()
        output.close()

        if printprg:
            print "%d/%d (100%%) completed" % (total_rows,total_rows)
