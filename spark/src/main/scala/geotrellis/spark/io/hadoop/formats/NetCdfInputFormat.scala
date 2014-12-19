package geotrellis.spark.io.hadoop.formats

import org.joda.time.{DateTimeZone, DateTime}

object NetCdfInputFormat {

  val Type = 'T'
  val Year = 'Y'
  val Month = 'M'
  val Day = 'D'

  val Seconds = "seconds"
  val Minutes = "minutes"
  val Hours = "hours"
  val Days = "days"
  val Months = "months"
  val Years = "years"

  val validTimeTypes = Seq(Seconds, Minutes, Hours, Days, Months, Years)

  val DefaultDateTimeFormat =
    s"$Type$Type$Type$Type*******$Year$Year$Year$Year*$Day$Day*$Month$Month"


  /**
    * This method reads the type and date from a NetCdf time units string.
    *
    * An example of such as string is "days since 1950-01-01".
    *
    * This method takes the following parameters:
    *
    * @param input The input string to be parsed.
    *
    * @param dateTimeFormat The format for the date time format.
    * This String needs to specify where the type is, and where
    * the year is declared. It can also declare the months and days.
    * For our example string above the DateTimeFormat should look be
    * "TTTT*******YYYY*MM*DD".
    *
    * @param yearOffset The year offset for the date time.
    *
    * @param monthffset The month offset for the date time.
    *
    * @param dayOffset The day offset for the date time.
    *
    * @return A tuple with the type as a string and the date time object.
    */
  def readTypeAndDate(
    input: String,
    dateTimeFormat: String = DefaultDateTimeFormat,
    yearOffset: Int = 0,
    monthOffset: Int = 0,
    dayOffset: Int = 0): (String, DateTime) = {
    validateDateTimeFormat(dateTimeFormat)

    val zipped = input.zip(dateTimeFormat)

    def parseFormatString(zipped: Seq[(Char, Char)], flag: Char) =
      zipped.filter(_._2 == flag).map(_._1).mkString

    val timeType = parseFormatString(zipped, Type).toLowerCase

    val year = parseFormatString(zipped, Year).toInt

    val monthString = parseFormatString(zipped, Month)
    val month = if (monthString.isEmpty) 0 else monthString.mkString.toInt

    val dayString = parseFormatString(zipped, Day)
    val day = if (dayString.isEmpty) 0 else dayString.mkString.toInt

    if (!validTimeTypes.contains(timeType)) invalidTimeType(timeType)

    val base = new DateTime(
      year + yearOffset,
      month + monthOffset,
      day + dayOffset,
      0, 0, 0, DateTimeZone.UTC)

    (timeType, base)
  }

  private def validateDateTimeFormat(dateTimeFormat: String) = {
    val hasType = dateTimeFormat.contains("TTTT")
    val hasYear = dateTimeFormat.contains("YYYY")

    if (!hasType || !hasYear) throw new IllegalArgumentException(
      s"Bad date time format string: $dateTimeFormat"
    )
  }

  private def invalidTimeType(timeType: String) = throw new IllegalArgumentException(
    s"The type $timeType is not recognized."
  )

/**
  * Increments the base date given by v time-type time steps.
  *
  * @param timeType The type of the time step.
  * @param v The float value of the number of time-type time steps.
  * @param base The date to be incremented.
  *
  * @return The incremented DateTime.
  */
  def incrementDate(timeType: String, v: Double, base: DateTime): DateTime =
    timeType match {
      case Seconds => base.plusSeconds(v.toInt)
      case Minutes => base.plusMinutes(v.toInt).plusSeconds((v % 1 * 60).round.toInt)
      case Hours => base.plusHours(v.toInt).plusMinutes((v % 1 * 60).round.toInt)
      case Days => base.plusDays(v.toInt).plusHours((v % 1 * 24).round.toInt)
      case Months => base.plusMonths(v.toInt)
      case Years => base.plusYears(v.toInt).plusMonths((v % 1 * 12).round.toInt)
      case _ => invalidTimeType(timeType)
    }

}

class NetCdfInputFormat(
  val baseDateMetaDataKey: String,
  val dateTimeFormat: String,
  val yearOffset: Int = 0,
  val monthOffset: Int = 0,
  val dayOffset: Int = 0
) extends Serializable

object DefaultNetCdfInputFormat extends NetCdfInputFormat(
  "Time#units",
  NetCdfInputFormat.DefaultDateTimeFormat
)
