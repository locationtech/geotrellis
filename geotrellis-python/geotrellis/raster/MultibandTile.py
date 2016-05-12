from geotrellis.raster.CellGrid import CellGrid
#from geotrellis.raster.ArrayMultibandTile import ArrayMultibandTile

def generateCodec():
    from geotrellis.spark.io.avro.codecs.TileCodecs import MultibandTileCodec
    return MultibandTileCodec()

class MultibandTile(CellGrid):
    implicits = {"AvroRecordCodec": generateCodec}

    def subsetBands(self, *bandSequence):
        if len(bandSequence) == 1 and isinstance(bandSequence[0], list):
            bandSequence = bandSequence[0]
        return self._subsetBands(bandSequence)

    @staticmethod
    def applyStatic(*bands):
        from geotrellis.raster.ArrayMultibandTile import ArrayMultibandTile
        if len(bands) == 1 and isinstance(bands[0], list):
            bands = bands[0]
        return ArrayMultibandTile.applyStatic(bands)
