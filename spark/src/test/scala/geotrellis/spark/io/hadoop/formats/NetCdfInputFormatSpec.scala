package geotrellis.spark.io.hadoop.formats

import org.scalatest.{FunSpec, Matchers}

import org.joda.time.{DateTimeZone, DateTime}

class NetCdfInputFormatSpec extends FunSpec with Matchers {

  describe("Type and Date reader") {

    val DateTime19500101 = new DateTime(1950, 1, 1, 0, 0, 0, DateTimeZone.UTC)

    def testWithDefaultDate(timeType: String) = {
      val (typ, date) = NetCdfInputFormat.readTypeAndDate(
        s"$timeType since 1950-01-01",
        "T" * timeType.length + "*******YYYY*DD*MM"
      )

      typ should be (timeType)
      date should be (DateTime19500101)
    }

    it("should read input with the default date time format and seconds type") {
      testWithDefaultDate("seconds")
    }

    it("should read input with the default date time format and minutes type") {
      testWithDefaultDate("minutes")
    }

    it("should read input with the default date time format and hours type") {
      testWithDefaultDate("hours")
    }

    it("should read input with the default date time format and days type") {
      testWithDefaultDate("days")
    }

    it("should read input with the default date time format and months type") {
      testWithDefaultDate("months")
    }

    it("should read input with the default date time format and years type") {
      testWithDefaultDate("years")
    }

    it("should read input with date with offsets") {
      val (typ, date) = NetCdfInputFormat.readTypeAndDate(
        "days since 1950-01-01",
        "TTTT*******YYYY*DD*MM",
        5, 3, 3
      )

      typ should be ("days")
      date should be (new DateTime(1955, 4, 4, 0, 0, 0, DateTimeZone.UTC))
    }

    it("should read input with other formatting") {
      val (typ, date) = NetCdfInputFormat.readTypeAndDate(
        "1971-3-3 with DaYs",
        "YYYY*M*D******TTTT"
      )

      typ should be ("days")
      date should be (new DateTime(1971, 3, 3, 0, 0, 0, DateTimeZone.UTC))
    }

  }

  describe("Date Incrementer") {

    val base = new DateTime(2000, 1, 1, 0, 0, 0, DateTimeZone.UTC)

    it("should increment date with correct number of seconds") {
      val res = NetCdfInputFormat.incrementDate("seconds", 200.1337, base)
      res should be (new DateTime(2000, 1, 1, 0, 3, 20, DateTimeZone.UTC))
    }

    it("should increment date with correct number of minutes") {
      val res = NetCdfInputFormat.incrementDate("minutes", 200.1, base)
      res should be (new DateTime(2000, 1, 1, 3, 20, 6, DateTimeZone.UTC))
    }

    it("should increment date with correct number of hours") {
      val res = NetCdfInputFormat.incrementDate("hours", 25.5, base)
      res should be (new DateTime(2000, 1, 2, 1, 30, 0, DateTimeZone.UTC))
    }

    it("should increment date with correct number of days") {
      val res = NetCdfInputFormat.incrementDate("days", 25.5, base)
      res should be (new DateTime(2000, 1, 26, 12, 0, 0, DateTimeZone.UTC))
    }

    it("should increment date with correct number of months") {
      val res = NetCdfInputFormat.incrementDate("months", 39.9, base)
      res should be (new DateTime(2003, 4, 1, 0, 0, 0, DateTimeZone.UTC))
    }

    it("should increment date with correct number of years") {
      val res = NetCdfInputFormat.incrementDate("years", 40.5, base)
      res should be (new DateTime(2040, 7, 1, 0, 0, 0, DateTimeZone.UTC))
    }

  }

}
