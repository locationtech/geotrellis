#geotrellis.macros

Because these macros are converted to source code at compile time, this module is the first module to be compiled - the contents of its compilation inform the compiler as it then works its way through the rest of GeoTrellis codebase. For specifics of how, exactly, macros are used, read about their use in [`geotrellis.raster`](../../../../../raster/src/main/scala/geotrellis/raster).

They're pretty simple, so you can probably get a good idea of what they do by looking at their sole file: 'macros.scala'.