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

class Layer:
    @staticmethod
    def fromPath(path):
        #Determine how to read the layer
        if path.endswith(".arg"):
            path = path[:-4] + ".json"
            if not os.path.exists(path):
                log.error("Could not find arg metadata file at " + path)
        if path.endswith(".json"):
            #Read metadata
            jf = open(path)
            meta = json.load(jf)
            jf.close()

            layer_type = meta["type"]
            if "path" in meta:
                data_path = meta["path"]
            else:
                data_path = path[:-5] + ".arg"

            if layer_type == "geotiff":
                if not data_path:
                    data_path = "%s.tif" % path[:-5]
                return GdalLayer(meta,data_path)
            # elif layer_type == "tiled":
            #     if not data_path:
            #         data_path = path[:-5]
            #     return TiledLayer(meta,data_path)
            # elif layer_type == "constant":
            #     return ConstantLayer(meta,data_path)
            elif layer_type == "arg":
                return ArgLayer(meta,path,data_path)
            else:
                log.error("Layer type %s in file %s is not support" % (layer_type,path))
        else:
            return GdalLayer(path)

    def init_data(self):
        raise "Must override in child class"

    def close(self):
        raise "Must override in child class"        

    def data_type(self):
        raise "Must override in child class"
    
    def raster_extent(self):
        raise "Must override in child class"

    def no_data_value(self):
        raise "Must override in child class"

    def read_row(self,row,offset=0,size=None):
        raise "Must override in child class"

    def arg_metadata(self):
        raise "Must override in child class"

    @staticmethod
    def get_layer_name(path):
        return '.'.join(os.path.basename(path).split('.')[:-1])

    def write_metadata(self, path, data_path, name=None, data_type = None, additional = None):
        if not name:
            name = Layer.get_layer_name(data_path)

        m = self.arg_metadata()
        m['layer'] = name
        if data_type:
            m['datatype'] = data_type
        if additional:
            m = dict(m.items() + additional.items())

        with file(path,'w') as mdf:
            mdf.write(
                json.dumps(m,
                           sort_keys=True,
                           indent=4,
                           separators=(',',': ')) + '\n')

    def write_arg(self,
                  data_path,
                  data_type = None,
                  window = None,
                  printprg = False,
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
        this_data_type = self.data_type()
        raster_extent = self.raster_extent()

        if not data_type:
            data_type = this_data_type
            
        # Process the windows for clipping
        if window is None:
            window = (0, 0, raster_extent.cols, raster_extent.rows)

        if len(window) != 4:
            log.error('Invalid window: %s' % window)

        start_col, end_col = window[0], window[2]
        total_cols =  end_col - start_col

        start_row, end_row = window[1], window[3]
        total_rows = end_row - start_row

        ndv = self.no_data_value()

        # if NoData is 128 and type is byte, 128 is read in as -128
        if this_data_type == 'int8' and ndv == 128:
            ndv = -128

        arg_no_data = nodata_for_fmt(data_type)

        psize = int(total_rows / 100)

        output = file(data_path, 'wb')
        writer = ArgWriter(output,data_type,verify)

        # If needed, add NoData values to the start or end of the row
        prerow = [arg_no_data] * (0 - start_col)
        start_col = max(0,start_col)
        postrow = [arg_no_data] * (end_col - raster_extent.cols)
        total_cols_to_scan = min(end_col, raster_extent.cols) - start_col

        for row in xrange(start_row,end_row):
            if printprg and psize != 0 and row % psize == 0:
                row_progress = row - start_row
                sys.stdout.write('%d/%d (%d%%) completed\r' % (row_progress,
                                                               total_rows,
                                                               row_progress*100/total_rows))
                sys.stdout.flush()
            output.flush()

            if row < 0 or row >= raster_extent.rows:
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

    def write_tiled(self,
                    tile_dir,
                    metadata_path,
                    layer_name,
                    tile_row_size,
                    tile_col_size,
                    data_type = None,
                    printprg = True,
                    verify = True):
        raster_extent = self.raster_extent()

        ntilecols = int(math.ceil(raster_extent.cols / float(tile_col_size)))
        ntilerows = int(math.ceil(raster_extent.rows / float(tile_row_size)))

        nrows = ntilerows * tile_row_size
        ncols = ntilecols * tile_col_size

        metadata = self.arg_metadata()
        tile_metadata = metadata.copy()

        tile_metadata['type'] = 'tiled'
        tile_metadata['layer'] = layer_name
        tile_metadata['path'] = os.path.basename(tile_dir)

        tile_metadata['tile_base'] = layer_name
        tile_metadata['layout_cols'] = str(ntilecols)
        tile_metadata['layout_rows'] = str(ntilerows)
        tile_metadata['pixel_cols'] = str(tile_col_size)
        tile_metadata['pixel_rows'] = str(tile_row_size)
        tile_metadata['cols'] = str(ncols)
        tile_metadata['rows'] = str(nrows)

        # Make the directory that will hold the tiles
        os.makedirs(tile_dir)

        with file(metadata_path,'w') as mdf:
            mdf.write(
                json.dumps(tile_metadata,
                           sort_keys=True,
                           indent=4,
                           separators=(',',': ')) + '\n')

        tile_name_ft = '%s_%%d_%%d' % layer_name
        tile_path_ft = os.path.join(tile_dir,'%s.arg' % tile_name_ft)

        total_tiles = ntilerows * ntilecols

        maxy = raster_extent.extent.ymax
        minx = raster_extent.extent.xmin
        cellwidth = raster_extent.cellwidth
        cellheight = raster_extent.cellheight

        for row in xrange(0,ntilerows):
            ystart = row * tile_row_size
            yend = ystart + tile_row_size

            for col in xrange(0,ntilecols):
                tileindex = col + row*ntilecols + 1
                p = int(tileindex * 100.0 / total_tiles)

                sys.stdout.write('Tile %d/%d (%d%%)\n' %
                                 (tileindex,total_tiles,p))
                sys.stdout.flush()

                xstart = col * tile_col_size
                xend = xstart + tile_col_size

                # Get tile extent
                xmin = minx + xstart * cellwidth
                xmax = xmin + (cellwidth * tile_col_size)
                ymax = maxy - (cellheight * ystart)
                ymin = ymax - (cellheight * tile_row_size)

                filename = tile_path_ft % (col,row)

                newmetadata = metadata.copy()
                # Shift xmin and recalculate xmax
                newmetadata['layer'] = tile_name_ft % (col,row)
                newmetadata['xmin'] = xmin
                newmetadata['ymin'] = ymin
                newmetadata['xmax'] = xmax
                newmetadata['ymax'] = ymax
                newmetadata['rows'] = tile_row_size
                newmetadata['cols'] = tile_col_size

                self.write_arg(filename, window = (xstart,ystart,xend,yend), verify = verify)

                metadata_file = filename[0:-4] + '.json'
                with file(metadata_file,'w') as mdf:
                    mdf.write(
                        json.dumps(newmetadata,
                                   sort_keys=True,
                                   indent=4,
                                   separators=(',',': ')) + '\n')

class ArgLayer(Layer):
    def __init__(self,meta,meta_path,data_path):
        self.meta = meta
        self.meta_path = meta_path
        self.data_path = data_path
        
        self.layer_type = meta["type"]
        
        self._data_type = meta["datatype"]
        self._no_data_value = nodata_map[self._data_type]
        self.name = meta["layer"]

        if "path" in meta:
            self.path = meta["path"]
        else:
            self.path = meta_path[:-4] + ".arg"

        self.size = datatype_size_map[self._data_type]

        xmin = float(meta["xmin"])
        ymin = float(meta["ymin"])
        xmax = float(meta["xmax"])
        ymax = float(meta["ymax"])
        extent = Extent(xmin,ymin,xmax,ymax)

        cw = float(meta["cellwidth"])
        ch = float(meta["cellheight"])

        cols = int(meta["cols"])
        rows = int(meta["rows"])

        self._raster_extent = RasterExtent(extent,cw,ch,cols,rows)
        
        self.epsg = meta["epsg"]
        self.xskew = meta["xskew"]
        self.yskew = meta["yskew"]

        self.fchar = inp_datatype_map[self._data_type]

    def init_data(self):
        self.data_file = open(self.data_path, "rb")

    def close(self):
        self.data_file.close()

    def data_type(self):
        return self._data_type
    
    def raster_extent(self):
        return self._raster_extent

    def no_data_value(self):
        return self._no_data_value

    def read_row(self,row,offset=0,cols=None):
        if not cols:
            cols = self.raster_extent().cols
        read_length = int(cols*(math.ceil(self.size/8)))
        self.seek(offset,row)
        data = self.data_file.read(read_length)
        endian = '>'
        inputfmt = "%s%d%s" % (endian,cols,self.fchar)
        return list(struct.unpack(inputfmt,data))

    def arg_metadata(self):
        re = self.raster_extent()
        extent = re.extent
        return {
            'type': 'arg',
            'datatype': self.data_type(),
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
            'epsg': int(self.epsg)
        }

    def seek(self,col,row):
        pos = ((row * self.raster_extent().cols) + col) * math.ceil(self.size/8)
        self.data_file.seek(pos)

class GdalLayer(Layer):
    """
    Represents one band in a raster as read by GDAL.
    """
    def __init__(self, path, band = 1):
        self.path = path
        self.band = band

    def init_data(self):
        self.dataset = gdal.Open(self.path, GA_ReadOnly)

        bands = self.dataset.RasterCount
        if bands < self.band:
            log.error("This raster does not contain a band number %d" % band)
        self.band = self.dataset.GetRasterBand(self.band)

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

        self._raster_extent = RasterExtent(extent,abs(xres),abs(yres),cols,rows)

        self._data_type = to_datatype_arg(self.band.DataType)
        self.fchar = to_datatype_str(self.band.DataType)

    def close(self):
        self.dataset = None

    def data_type(self):
        return self._data_type
    
    def raster_extent(self):
        return self._raster_extent

    def no_data_value(self):
        ndv = self.band.GetNoDataValue()
        # if NoData is 128 and type is byte, 128 is read in as -128
        if self.data_type() == 'int8' and ndv == 128:
            ndv = -128
        return ndv

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

    def arg_metadata(self):
        re = self.raster_extent()
        extent = re.extent

        return {
            'type': 'arg',
            'datatype': self.data_type(),
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
            'epsg': int(self.epsg)
        }
