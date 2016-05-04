from geotrellis.spark.io.package_scala import ExceptionWithCause

class GeoAttrsError(ExceptionWithCause):
    def __init__(self, msg, **kwargs):
        ExceptionWithCause.__init__(self, msg, **kwargs)
