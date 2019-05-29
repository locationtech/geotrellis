Migrating to GeoTrellis 3.0
----------------------------

With the release of of GeoTrellis 3.0, some major API changes have occurred.
This document seeks to go over these major changes, and how one can update
their preexisting code to be compatible with 3.0.

Simplified Imports
###################

One change brought about in 3.0 is the simplification of imports
in both the ``raster`` and ``vector`` packages. Before, it was not
uncommon to see files with a long list of imports such as this:

.. code:: scala
  import geotrellis.vector._
  import geotrellis.vector.io.wkb.WKB
  import geotrellis.raster._
  import geotrellis.raster.reproject._
  import geotrellis.raster.resample.ResampleMethods


With the simplification of imports, the above is now:

.. code:: scala
  import geotrellis.vector._
  import geotrellis.raster._


This section will go over what is now available at the ``geotrellis.raster`` and
``geotrellis.vector`` levels.

geotrellis.vector
==================

The following implicits and types are now available in ``geotrellis.vector``

**Implicits:**
  - ``io.json``
  - ``io.wkb``
  - ``io.wkt``

**Types:**
  - ``io.wkb.WKB``
  - ``io.wkt.WKT``

geotrellis.raster
==================

These implicit methods can be used on ``Tile``\s, ``MultibandTile``\s,
``TileFeature[T, D]``\s, ``Raster[T]``\s, or ``TileFeature[Raster[T], D]``\s.

**Implicits:**
  - ``crop``
  - ``io.json``
  - ``mask``
  - ``merge``
  - ``prototype``
  - ``reproject``
  - ``split``
  - ``transform``

The following types and objects are now directly available at the
``geotrellis.raster`` level.

**Types:**
  - ``histogram.Histogram[T]``
  - ``histogram.MutableHistogram[T]``
  - ``histogram.StreamingHistogram``
  - ``histogram.IntHistogram``
  - ``histogram.DoubleHistogram``
  - ``rasterize.CellValue``
  - ``render.ColorMap``
  - ``render.ColorMaps``
  - ``render.ColorRamp``
  - ``render.ColorRamps``
  - ``render.RGB``
  - ``render.RGBA``
  - ``resample.ResampleMethod``
  - ``mapalgebra.focal.Neighborhood``
  - ``mapalgebra.focal.TargetCell``
  - ``stitch.Stitcher``

In addition to the moved types and objects, new objects have been created to
allow for easier access of certain types.

- ``CropOptions`` alias of ``crop.Crop.Options``
- ``RasterizerOptions`` alias of ``rasterize.Rasterizer.Options``
- ``ColorMapOptions`` alias of ``render.ColorMap.Options``
- ``SplitOptions`` alias of ``split.Split.Options``

Two enum objects have also been created in ``geotrellis.raster``.

- ``ResampleMethods`` object that contains all of the resample methods.
- ``Neighborhoods`` object that contains all of the neighborhoods.
