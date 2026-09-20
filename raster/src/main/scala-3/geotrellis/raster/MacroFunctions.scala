/*
 * Copyright 2016 Azavea
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


package geotrellis.raster

import geotrellis.macros.{NoDataMacros, TypeConversionMacros}

trait MacroFunctions {
  inline def isNoData(inline i: Int): Boolean = ${ NoDataMacros.isNoDataInt_impl('i) }
  inline def isNoData(inline f: Float): Boolean = ${ NoDataMacros.isNoDataFloat_impl('f) }
  inline def isNoData(inline d: Double): Boolean = ${ NoDataMacros.isNoDataDouble_impl('d) }

  inline def isData(inline i: Int): Boolean = ${ NoDataMacros.isDataInt_impl('i) }
  inline def isData(inline f: Float): Boolean = ${ NoDataMacros.isDataFloat_impl('f) }
  inline def isData(inline d: Double): Boolean = ${ NoDataMacros.isDataDouble_impl('d) }

  inline def b2ub(inline n: Byte): Byte = ${ TypeConversionMacros.b2ub_impl('n) }
  inline def b2s(inline n: Byte): Short = ${ TypeConversionMacros.b2s_impl('n) }
  inline def b2us(inline n: Byte): Short = ${ TypeConversionMacros.b2us_impl('n) }
  inline def b2i(inline n: Byte): Int = ${ TypeConversionMacros.b2i_impl('n) }
  inline def b2f(inline n: Byte): Float = ${ TypeConversionMacros.b2f_impl('n) }
  inline def b2d(inline n: Byte): Double = ${ TypeConversionMacros.b2d_impl('n) }

  inline def ub2b(inline n: Byte): Byte = ${ TypeConversionMacros.ub2b_impl('n) }
  inline def ub2s(inline n: Byte): Short = ${ TypeConversionMacros.ub2s_impl('n) }
  inline def ub2us(inline n: Byte): Short = ${ TypeConversionMacros.ub2us_impl('n) }
  inline def ub2i(inline n: Byte): Int = ${ TypeConversionMacros.ub2i_impl('n) }
  inline def ub2f(inline n: Byte): Float = ${ TypeConversionMacros.ub2f_impl('n) }
  inline def ub2d(inline n: Byte): Double = ${ TypeConversionMacros.ub2d_impl('n) }

  inline def s2b(inline n: Short): Byte = ${ TypeConversionMacros.s2b_impl('n) }
  inline def s2ub(inline n: Short): Byte = ${ TypeConversionMacros.s2ub_impl('n) }
  inline def s2us(inline n: Short): Short = ${ TypeConversionMacros.s2us_impl('n) }
  inline def s2i(inline n: Short): Int = ${ TypeConversionMacros.s2i_impl('n) }
  inline def s2f(inline n: Short): Float = ${ TypeConversionMacros.s2f_impl('n) }
  inline def s2d(inline n: Short): Double = ${ TypeConversionMacros.s2d_impl('n) }

  inline def us2b(inline n: Short): Byte = ${ TypeConversionMacros.us2b_impl('n) }
  inline def us2ub(inline n: Short): Byte = ${ TypeConversionMacros.us2ub_impl('n) }
  inline def us2s(inline n: Short): Short = ${ TypeConversionMacros.us2s_impl('n) }
  inline def us2i(inline n: Short): Int = ${ TypeConversionMacros.us2i_impl('n) }
  inline def us2f(inline n: Short): Float = ${ TypeConversionMacros.us2f_impl('n) }
  inline def us2d(inline n: Short): Double = ${ TypeConversionMacros.us2d_impl('n) }

  inline def i2b(inline n: Int): Byte = ${ TypeConversionMacros.i2b_impl('n) }
  inline def i2ub(inline n: Int): Byte = ${ TypeConversionMacros.i2ub_impl('n) }
  inline def i2s(inline n: Int): Short = ${ TypeConversionMacros.i2s_impl('n) }
  inline def i2us(inline n: Int): Short = ${ TypeConversionMacros.i2us_impl('n) }
  inline def i2f(inline n: Int): Float = ${ TypeConversionMacros.i2f_impl('n) }
  inline def i2d(inline n: Int): Double = ${ TypeConversionMacros.i2d_impl('n) }

  inline def f2b(inline n: Float): Byte = ${ TypeConversionMacros.f2b_impl('n) }
  inline def f2ub(inline n: Float): Byte = ${ TypeConversionMacros.f2ub_impl('n) }
  inline def f2s(inline n: Float): Short = ${ TypeConversionMacros.f2s_impl('n) }
  inline def f2us(inline n: Float): Short = ${ TypeConversionMacros.f2us_impl('n) }
  inline def f2i(inline n: Float): Int = ${ TypeConversionMacros.f2i_impl('n) }
  inline def f2d(inline n: Float): Double = ${ TypeConversionMacros.f2d_impl('n) }

  inline def d2b(inline n: Double): Byte = ${ TypeConversionMacros.d2b_impl('n) }
  inline def d2ub(inline n: Double): Byte = ${ TypeConversionMacros.d2ub_impl('n) }
  inline def d2s(inline n: Double): Short = ${ TypeConversionMacros.d2s_impl('n) }
  inline def d2us(inline n: Double): Short = ${ TypeConversionMacros.d2us_impl('n) }
  inline def d2i(inline n: Double): Int = ${ TypeConversionMacros.d2i_impl('n) }
  inline def d2f(inline n: Double): Float = ${ TypeConversionMacros.d2f_impl('n) }
}
