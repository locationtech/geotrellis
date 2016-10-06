GeoTrellis uses [Apache Avro](https://avro.apache.org/) serialization when storing its tiled map layers. While GeoTrellis is a Scala project Avro provides libraries for multiple languages, including python. This project is to implement a python library that will read and write persisted GeoTrellis layers and present them as [NumPy](http://www.numpy.org/) datatypes that can be used by [rasterio](https://github.com/mapbox/rasterio). Once the basic translation is supported the next step would be to read GeoTrellis layer in a distributed Python Spark job and use rasterio to perform geospatial computations.

Interoperability between GeoTrellis and rasterio will allow for complex, multi-stage geospatial workflows that do not suffer the usual performance penalties when working mixed Spark Scala/Python environment, expanding the utility of both libraries.

#### Dependencies

- [Apache Avro](https://avro.apache.org/)
- [Avro Json Serializer](https://github.com/linkedin/python-avro-json-serializer)
- [NumPy](http://www.numpy.org/)
- [rasterio](https://github.com/mapbox/rasterio)
- [pytz](https://github.com/newvem/pytz)
- [pypng](https://github.com/drj11/pypng)
- [Spec](https://github.com/bitprophet/spec) for testing

#### Notes on compatibility

The library will work with the following versions of dependencies (the command is `python setup.py install` under the main folder of the repo):
- https://github.com/shiraeeshi/spec/tree/feature/options
- https://github.com/shiraeeshi/python-avro-json-serializer/tree/feature/fullname
- https://github.com/shiraeeshi/avro/tree/temp/with180version (The `setup.py` file resides in `lang/py` subdirectory)

There are pending pull requests made to original repositories, the library will be compatible with original versions as soon as they accept those pull requests.

#### Testing

The testing framework depends on:
- `SPARK_HOME` environment variable
- the existence of `geotrellis-python/dist/Geotrellis-0.1-py2.7.egg` file. You can create it by running 

`python setup.py install`

from inside `geotrellis-python` directory.

In order to run the tests, issue the following comand:

`spec --with-inherited --with-tools-decorators-only` 

from inside `geotrellis-python` directory.

#### Sample server

The landsat-django-server project (https://github.com/shiraeeshi/landsat-django-server) shows how to create a tile server with GeoTrellis ingested tiles.

