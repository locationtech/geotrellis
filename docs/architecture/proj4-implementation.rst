Proj4 Implementation
********************

GeoTrellis relies heavily on the Proj4J library, which in turn borrows much
of its implementation from the `proj.4 <https://github.com/OSGeo/proj.4>`__
c library. There is a correspondence between proj.4 functions and Proj4J
classes, although it is not direct since C and Java coding conventions vary.

.. note::  Currently the GeoTrellis team maintains a fork of
           Proj4J in the GeoTrellis source repository, rather than
           relying on an official release. This includes some added
           projection parameters and other improvements to make proj4j
           more suitable for use in a distributed context such as
           marking appropriate objects with the ``java.io.Serializable``
           marker interface.

The format of parameters passed to proj.4 command line tools is also
supported by Proj4J, although it is not 100% compatible with all parameters.
In some cases invalid parameters may cause exceptions, in others they may
cause incorrect results.

What makes a Coordinate Reference System?
-----------------------------------------

Any time you load a coordinate reference system in Proj4J you are creating
an instance of the ``CoordinateReferenceSystem`` class.
``CoordinateReferenceSystem`` is a wrapper around two types:

-  ``Datum`` which defines a `coordinate system anchored to the Earth's
   surface <https://en.wikipedia.org/wiki/Geodetic_datum>`__
-  ``Projection`` which defines the
   `mapping <https://en.wikipedia.org/wiki/Map_projection>`__ we are
   using between that curved surface and 2-dimensional space.
   Projections in Proj4J support many parameters including units to be
   used, axis reordering, and some that are specific to individual
   projection types.

While it is technically possible to create a ``CoordinateReferenceSystem``
by manipulating ``Projection`` and ``Datum`` instances in Java code, typical
usage is to use the ``Proj4Parser`` class to create one from proj.4
parameters.

Note that in contrast to the Proj4J implementation of a
``CoordinateReferenceSystem`` containing objects, all the coordinate system
parameters are contained in the ``PJ`` struct in proj.4.

Datum
-----

A ``Datum`` in Proj4J contains a reference ``Ellipsoid`` (model of the Earth
as a `mathematical surface <https://en.wikipedia.org/wiki/Ellipsoid>`__ with
known equatorial radius and polar radius) and defines a mathematical
transform to and from WGS84 latitude/longitude coordinates. This can be a
simple 3-parameter transform (affine translation,) a `7-parameter transform
<https://en.wikipedia.org/wiki/Helmert_transformation>`__ (affine translate
+ rotate + scale,) or a ``Grid`` mapping part of the world's surface to
latitude/longitude. Proj4's ``+ellps`` ``+datum`` ``+nadgrids`` and
``+towgs84`` parameters all affect the ``Datum`` in the parsed projection.
In proj.4 the datum information is flattened into the ``PJ`` struct rather
than separated out to a separate entity.

Projection
==========

A ``Projection`` in Proj4J represents a formula for projecting geodetic
coordinates (latitude/longitude/distance from center of the earth) to some
2D coordinate system. The Java encoding of these is a ``Projection`` base
class with subclasses for each supported formula; eg ``MercatorProjection``.
The ``+proj`` parameter determines which projection class is instantiated.
Aside from this and the datum parameters, all supported parameters affect
fields of the ``Projection``. In proj.4 the projection function is
represented as pointers to setup, transform, inverse transform, and teardown
functions, with these families of functions being implemented in one C
source file per projection.

EPSG Codes
==========

The EPSG database is released as a collection of XML files and periodically
updated. The proj4 project seems to have automatic means to convert the XML
parameter definitions to proj4 parameter lists, and ships a file containing
one epsg coordinate system definition per line in ``nad/epsg``. For Proj4J
we have simply copied this file directly.

Testing
=======

The tests for Proj4J are mostly Scala ports of JUnit tests with
hand-constructed inputs and outputs. Of particular interest are the tests in
the ``MetaCRSTest`` which reads input parameters and expected results from
CSV files, making it a little easier to manage large test suites. The
``GenerateTestCases.scala`` file in the tests directory uses the ``cs2cs``
command line tool to perform sample conversions in each supported coordinate
reference system for cross-validation. If you're looking to improve Proj4J's
consistency with proj.4 a good place to start is the ``proj4-epsg.csv``
dataset in ``src/test/resources/`` - changing ``failing`` to ``passing`` on
any line in that file will generate one test failure that you can
investigate. Furthermore there are tests marked with the ScalaTest
``ignore`` function in many of the other test suites that would ideally be
enabled and passing.

Further Reading
---------------

For some general information on coordinate systems and geospatial
projections, see:

-  `Snyder, 1987: Map projection; a working
   manual <http://pubs.er.usgs.gov/usgspubs/pp/pp1395>`__
-  `Map
   projections <http://www.progonos.com/furuti/MapProj/Normal/TOC/cartTOC.html>`__
-  `proj.4 Wiki <https://github.com/osgeo/proj.4/wiki>`__
