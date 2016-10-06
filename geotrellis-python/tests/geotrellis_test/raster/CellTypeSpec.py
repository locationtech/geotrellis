from __future__ import absolute_import
from spec import Spec
from nose import tools
from nose.tools import ok_
import sys

from geotrellis.raster.CellType import (CellType,
        ShortCellType, IntCellType, FloatCellType, DoubleCellType,
        DoubleUserDefinedNoDataCellType, DoubleConstantNoDataCellType,
        FloatUserDefinedNoDataCellType, FloatConstantNoDataCellType)

def _roundTrip(ct):
    string = str(ct)
    ctp = CellType.fromString(string)
    ok_(ctp == ct)

@tools.istest
class CellTypeSpec(Spec):
    @tools.istest
    def test_union(self):
        """should union cells correctly under various circumstances"""
        ok_(ShortCellType.union(IntCellType) is IntCellType)
        ok_(DoubleCellType.union(FloatCellType) is DoubleCellType)
        ok_(ShortCellType.union(ShortCellType) is ShortCellType)
        ok_(IntCellType.union(FloatCellType) is FloatCellType)
        ok_(FloatCellType.union(IntCellType) is FloatCellType)

    @tools.istest
    def test_intersect(self):
        """should intersect cells correctly under various circumstances"""
        ok_(ShortCellType.intersect(IntCellType) is ShortCellType)
        ok_(DoubleCellType.intersect(FloatCellType) is FloatCellType)
        ok_(ShortCellType.intersect(ShortCellType) is ShortCellType)
        ok_(IntCellType.intersect(FloatCellType) is IntCellType)
        ok_(FloatCellType.intersect(IntCellType) is IntCellType)

    @tools.istest
    def test_serialize01(self):
        """should serialize float64ud123"""
        _roundTrip(DoubleUserDefinedNoDataCellType(123))

    @tools.istest
    def test_serialize02(self):
        """should serialize float64ud123.3"""
        _roundTrip(DoubleUserDefinedNoDataCellType(123.3))

    @tools.istest
    def test_serialize03(self):
        """should serialize float64ud1e12"""
        _roundTrip(DoubleUserDefinedNoDataCellType(1e12))

    @tools.istest
    def test_serialize04(self):
        """should serialize float64ud-1e12"""
        _roundTrip(DoubleUserDefinedNoDataCellType(-1e12))

    @tools.istest
    def test_serialize05(self):
        """should serialize negative float64ud"""
        _roundTrip(DoubleUserDefinedNoDataCellType(-1.7E308))

    @tools.istest
    def test_serialize06(self):
        """should serialize Float.MinValue value float64ud"""
        _roundTrip(DoubleUserDefinedNoDataCellType(sys.float_info.min))

    @tools.istest
    def test_serialize07(self):
        """should serialize Float.MaxValue value float64ud"""
        _roundTrip(DoubleUserDefinedNoDataCellType(sys.float_info.max))

    @tools.istest
    def test_serialize08(self):
        """should serialize float64udInfinity"""
        _roundTrip(DoubleUserDefinedNoDataCellType(float("inf")))

    @tools.istest
    def test_serialize09(self):
        """should serialize float64ud-Infinity"""
        _roundTrip(DoubleUserDefinedNoDataCellType(-float("inf")))

    @tools.istest
    def test_serialize10(self):
        """should read float64udNaN as float64"""
        ok_(CellType.fromString(str(DoubleUserDefinedNoDataCellType(float("nan")))) is DoubleConstantNoDataCellType)

    @tools.istest
    def test_serialize11(self):
        """should serialize float32ud123"""
        _roundTrip(FloatUserDefinedNoDataCellType(float(123)))

    @tools.istest
    def test_serialize12(self):
        """should serialize float32ud123.3"""
        _roundTrip(FloatUserDefinedNoDataCellType(float(123.3)))

    @tools.istest
    def test_serialize13(self):
        """should serialize float32ud1e12"""
        _roundTrip(FloatUserDefinedNoDataCellType(float(1e12)))

    @tools.istest
    def test_serialize14(self):
        """should serialize float32ud-1e12"""
        _roundTrip(FloatUserDefinedNoDataCellType(float(-1e12)))

    @tools.istest
    def test_serialize15(self):
        """should serialize Float.MinValue value float32ud"""
        _roundTrip(FloatUserDefinedNoDataCellType(sys.float_info.min))

    @tools.istest
    def test_serialize16(self):
        """should serialize Float.MaxValue value float32ud"""
        _roundTrip(FloatUserDefinedNoDataCellType(sys.float_info.max))

    @tools.istest
    def test_serialize17(self):
        """should serialize float32udInfinity"""
        _roundTrip(FloatUserDefinedNoDataCellType(float("inf")))

    @tools.istest
    def test_serialize18(self):
        """should serialize float32ud-Infinity"""
        _roundTrip(FloatUserDefinedNoDataCellType(-float("inf")))

    @tools.istest
    def test_serialize19(self):
        """should read float32udNaN as float32"""
        ok_(CellType.fromString(str(FloatUserDefinedNoDataCellType(float("nan")))) == FloatConstantNoDataCellType)
