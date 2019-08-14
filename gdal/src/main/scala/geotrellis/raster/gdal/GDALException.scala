/*
 * Copyright 2019 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.gdal


/** Base class for all Exceptions involving GDAL. */
class GDALException(message: String, gdalErrorCode: Int) extends Exception(message)

/** Exception thrown when data could not be read from data source. */
class GDALIOException(message: String, gdalErrorCode: Int) extends GDALException(message, gdalErrorCode)

/** Exception thrown when the attributes of a data source are found to be bad. */
class MalformedDataException(message: String, gdalErrorCode: Int) extends GDALException(message, gdalErrorCode)

/** Exception thrown when the DataType of a data source is found to be bad. */
class MalformedDataTypeException(message: String, gdalErrorCode: Int) extends GDALException(message, gdalErrorCode)

/** Exception thrown when the projection of a data source is found to be bad. */
class MalformedProjectionException(message: String, gdalErrorCode: Int) extends GDALException(message, gdalErrorCode)
