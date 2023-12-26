import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Copyright (c) 2023 scintilla0 (<a href="https://github.com/scintilla0">https://github.com/scintilla0</a>)<br>
 * license MIT License <a href="http://www.opensource.org/licenses/mit-license.html">http://www.opensource.org/licenses/mit-license.html</a><br>
 * license GPL2 License <a href="http://www.gnu.org/licenses/gpl.html">http://www.gnu.org/licenses/gpl.html</a><br>
 * <br>
 * This class provides an assortment of date and time converting and calculation methods,
 * most of which have auto-parsing support using {@link #parseDate(Object)},
 * {@link #parseTime(Object)} and {@link #parse(Object)}.<br>
 * @version 1.1.5 - 2023-12-26
 * @author scintilla0
 */
public class DateTimeUtil {

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// date getter

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Parses an instance of a supported type into a <b>LocalDate</b> object.<br>
	 * Supports instances of: <b>LocalDateTime</b>, <b>LocalDate</b>, <b>Timestamp</b>, <b>Date</b>,
	 * <b>Calendar</b>, <b>String</b>, <b>Long(long)</b>, <b>Integer(int)</b>.<br>
	 * <b>String</b> arguments will be parsed with one of the preset formats,
	 * and will return {@code null} if fails to parse using any of those formats.<br>
	 * <b>Long(long)</b> arguments will be recognized same as <b>Timestamp</b>,
	 * while <b>Integer(int)</b> same as <b>String</b>.<br>
	 * Passing {@code null} will return {@code null}.<br>
	 * Passing an unsupported argument will throw a <b>DateTimeParseException</b>.
	 * @param source Target object to be parsed into <b>LocalDate</b>.
	 * @return Parsed <b>LocalDate</b> value.
	 */
	public static LocalDate parseDate(Object source) {
		LocalDate result = null;
		if (source == null) {
			return null;
		} else if (source instanceof String) {
			String sourceString = (String) source;
			if (!EmbeddedStringUtil.isNullOrBlank(sourceString)) {
				for (DateTimeFormatter format : PRESET_DATE_FORMAT.keySet()) {
					result = parseDate(sourceString, format);
					if (result != null) {
						break;
					}
				}
			}
			if (result == null) {
				result = parseDate_jp(sourceString);
			}
		} else if (source instanceof LocalDate) {
			result = LocalDate.from((LocalDate) source);
		} else if (source instanceof LocalDateTime) {
			result = ((LocalDateTime) source).toLocalDate();
		} else if (source instanceof Timestamp) {
			result = parseDate(((Timestamp) source).toLocalDateTime());
		} else if (source instanceof Date) {
			result = ((Date) source).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		} else if (source instanceof Calendar) {
			result = parseDate(((Calendar) source).getTime());
		} else if (source instanceof Long) {
			result = parseDate(new Timestamp((Long) source));
		} else if (source instanceof Integer) {
			result = parseDate(((Integer) source).toString());
		} else {
			throw new DateTimeParseException("Unparseable argument(s) passed in", source.toString(), 0);
		}
		return result;
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Parses a <b>String</b> char sequence into a <b>LocalDate</b> object with the specified format.<br>
	 * Returns {@code null} if an empty char sequence is passed in or fails to parse using the specified format.
	 * @param source Target <b>String</b> char sequence to be parsed into <b>LocalDate</b>.
	 * @param formatPattern Target date format presented by a <b>String</b> char sequence.
	 * @return Parsed <b>LocalDate</b> value.
	 */
	public static LocalDate parseDate(String source, String formatPattern) {
		return parseDate(source, DateTimeFormatter.ofPattern(formatPattern));
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Parses a <b>String</b> char sequence into a <b>LocalDate</b> object with the specified format.<br>
	 * Returns {@code null} if an empty char sequence is passed in or fail to parse using the specified format.
	 * @param source Target <b>String</b> char sequence to be parsed into <b>LocalDate</b>.
	 * @param format Target date format presented by a {@link DateTimeFormatter} object.
	 * @return Parsed <b>LocalDate</b> value.
	 */
	public static LocalDate parseDate(String source, DateTimeFormatter format) {
		if (EmbeddedStringUtil.isNullOrBlank(source)) {
			return null;
		}
		try {
			return LocalDate.parse(source, format);
		} catch (DateTimeParseException exception) {
			return null;
		}
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Parses a <b>String</b> char sequence into a <b>LocalDate</b> object with the format of historical chronology of Japan.<br>
	 * Returns {@code null} if an empty char sequence is passed in or fail to parse using the specified format.
	 * @param source Target <b>String</b> char sequence to be parsed into <b>LocalDate</b>.
	 * @return Parsed <b>LocalDate</b> value.
	 * @see #JP_ERA_NAME
	 */
	public static LocalDate parseDate_jp(String source) {
		if (EmbeddedStringUtil.isNullOrBlank(source)) {
			return null;
		}
		if (source.length() <= 2) {
			return null;
		}
		String eraName = source.substring(0, 2);
		EraYearSpan eraYearSpan = JP_ERA_NAME.get(eraName);
		if (eraYearSpan == null) {
			return null;
		}
		int indexOfYear = source.indexOf("年");
		if (indexOfYear == -1) {
			return null;
		}
		String year = source.substring(2, indexOfYear);
		if ("元".equals(year)) {
			year = "1";
		}
		try {
			year = String.valueOf(eraYearSpan.getBegin() + Integer.parseInt(year) - 1);
		} catch (NumberFormatException exception) {
			return null;
		}
		source = year + source.substring(indexOfYear);
		return parseDate(source, _DATE_SHORT_MD_CHAR);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Evaluates if the target object can be parsed into a valid <b>LocalDate</b> object.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source Target date object.
	 * @return {@code true} if successfully parsed.
	 */
	public static boolean isDate(Object source) {
		return parseDate(source) != null;
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Evaluates if the target char sequence can be parsed into a valid <b>LocalDate</b> object with the specified format.
	 * @param source Target <b>String</b> char sequence.
	 * @param formatPattern Target date format presented by a <b>String</b> char sequence.
	 * @return {@code true} if successfully parsed.
	 */
	public static boolean isDate(String source, String formatPattern) {
		return parseDate(source, formatPattern) != null;
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Evaluates if the target char sequence can be parsed into a valid <b>LocalDate</b> object with the specified format.
	 * @param source Target <b>String</b> char sequence.
	 * @param format Target date format presented by a {@link DateTimeFormatter} object.
	 * @return {@code true} if successfully parsed.
	 */
	public static boolean isDate(String source, DateTimeFormatter format) {
		return parseDate(source, format) != null;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// date calculating

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Retrieves the nearest date before the source date that matches the specified day of the week.<br>
	 * If the specified day of the week exceeds the valid range, the closest valid value will be used by default.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;atPreviousDateOfWeek("2002-07-21"(7), 5) -> [2002-07-19](5)
	 * &#9;atPreviousDateOfWeek("2002-07-21"(7), 7) -> [2002-07-14](7)</pre>
	 * @param source Source date object.
	 * @param dayOfWeek Target day of the week presented by an <b>int</b> value.
	 * @return Retrieved <b>LocalDate</b> value.
	 */
	public static LocalDate atPreviousDateOfWeek(Object source, int dayOfWeek) {
		return atDateOfWeekCore(source, parseDayOfWeek(dayOfWeek), -1);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Retrieves the nearest date before the source date that matches the specified day of the week.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;atPreviousDateOfWeek("2002-07-21"(7), {@link DayOfWeek#FRIDAY}) -> [2002-07-19](5)
	 * &#9;atPreviousDateOfWeek("2002-07-21"(7), {@link DayOfWeek#SUNDAY}) -> [2002-07-14](7)</pre>
	 * @param source Source date object.
	 * @param dayOfWeek Target day of the week presented by a {@link DayOfWeek} enum.
	 * @return Retrieved <b>LocalDate</b> value.
	 */
	public static LocalDate atPreviousDateOfWeek(Object source, DayOfWeek dayOfWeek) {
		return atDateOfWeekCore(source, dayOfWeek, -1);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Retrieves the nearest date after the source date that matches the specified day of the week.<br>
	 * If the specified day of the week exceeds the valid range, the closest valid value will be used by default.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;atNextDateOfWeek("2002-07-21"(7), 5) -> [2002-07-26](5)
	 * &#9;atNextDateOfWeek("2002-07-21"(7), 7) -> [2002-07-28](7)</pre>
	 * @param source Source date object.
	 * @param dayOfWeek Target day of the week presented by an <b>int</b> value.
	 * @return Retrieved <b>LocalDate</b> value.
	 */
	public static LocalDate atNextDateOfWeek(Object source, int dayOfWeek) {
		return atDateOfWeekCore(source, parseDayOfWeek(dayOfWeek), 1);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Retrieves the nearest date after the source date that matches the specified day of the week.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;atNextDateOfWeek("2002-07-21"(7), {@link DayOfWeek#FRIDAY}) -> [2002-07-26](5)
	 * &#9;atNextDateOfWeek("2002-07-21"(7), {@link DayOfWeek#SUNDAY}) -> [2002-07-28](7)</pre>
	 * @param source Source date object.
	 * @param dayOfWeek Target day of the week presented by a {@link DayOfWeek} enum.
	 * @return Retrieved <b>LocalDate</b> value.
	 */
	public static LocalDate atNextDateOfWeek(Object source, DayOfWeek dayOfWeek) {
		return atDateOfWeekCore(source, dayOfWeek, 1);
	}

	private static LocalDate atDateOfWeekCore(Object source, DayOfWeek dayOfWeek, int offSet) {
		LocalDate result = atDateInWeek(source, dayOfWeek);
		if (result == null) {
			return null;
		}
		if (compareDate(source, result) != (-offSet)) {
			result = plusWeeksToDate(result, offSet);
		}
		return result;
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Retrieves the date in the same week as the source date that matches the specified day of the week.<br>
	 * If the specified day of the week exceeds the valid range, the closest valid value will be used by default.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;atDateInWeek("2002-07-21"(7), 7) -> [2002-07-21](7)
	 * &#9;atDateInWeek("2002-07-21"(7), 0) -> [2002-07-21](7)
	 * &#9;atDateInWeek("2002-07-21"(7), 1) -> [2002-07-15](1)</pre>
	 * @param source Source date object.
	 * @param dayOfWeek Target day of the week presented by an <b>int</b> value.
	 * @return Retrieved <b>LocalDate</b> value.
	 */
	public static LocalDate atDateInWeek(Object source, int dayOfWeek) {
		return atDateInWeek(source, parseDayOfWeek(dayOfWeek));
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Retrieves the date in the same week as the source date that matches the specified day of the week.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;atDateInWeek("2002-07-21"(7), {@link DayOfWeek#SUNDAY}) -> [2002-07-21](7)
	 * &#9;atDateInWeek("2002-07-21"(7), {@link DayOfWeek#MONDAY}) -> [2002-07-15](1)</pre>
	 * @param source Source date object.
	 * @param dayOfWeek Target day of the week presented by a {@link DayOfWeek} enum.
	 * @return Retrieved <b>LocalDate</b> value.
	 */
	public static LocalDate atDateInWeek(Object source, DayOfWeek dayOfWeek) {
		LocalDate result = parseDate(source);
		if (result == null) {
			return null;
		}
		return result.with(dayOfWeek);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Retrieves the date in the same week as the source date that matches the specified day of the week.<br>
	 * If the specified day of the week exceeds the valid range, the closest valid value will be used by default.<br>
	 * <font color="#EE2222"><b>Assumes Sunday as the first day of the week.</b></font><br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;atDateInWeekSundayFirst("2002-07-21"(7), 7) -> [2002-07-21](7)
	 * &#9;atDateInWeekSundayFirst("2002-07-21"(7), 0) -> [2002-07-21](7)
	 * &#9;atDateInWeekSundayFirst("2002-07-21"(7), 1) -> [2002-07-22](1)</pre>
	 * @param source Source date object.
	 * @param dayOfWeek Target day of the week presented by an <b>int</b> value.
	 * @return Retrieved <b>LocalDate</b> value.
	 */
	public static LocalDate atDateInWeekSundayFirst(Object source, int dayOfWeek) {
		return atDateInWeekSundayFirst(source, parseDayOfWeek(dayOfWeek));
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Retrieves the date in the same week as the source date that matches the specified day of the week.<br>
	 * <font color="#EE2222"><b>Assumes Sunday as the first day of the week.</b></font><br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;atDateInWeekSundayFirst("2002-07-21"(7), {@link DayOfWeek#SUNDAY}) -> [2002-07-21](7)
	 * &#9;atDateInWeekSundayFirst("2002-07-21"(7), {@link DayOfWeek#MONDAY}) -> [2002-07-22](1)</pre>
	 * @param source Source date object.
	 * @param dayOfWeek Target day of the week presented by a {@link DayOfWeek} enum.
	 * @return Retrieved <b>LocalDate</b> value.
	 */
	public static LocalDate atDateInWeekSundayFirst(Object source, DayOfWeek dayOfWeek) {
		LocalDate result = parseDate(source);
		if (result == null) {
			return null;
		}
		long adjustment = result.getDayOfWeek().equals(DayOfWeek.SUNDAY) ? 1L : 0L;
		result = atDateInWeek(result, dayOfWeek);
		assert result != null;
		adjustment += result.getDayOfWeek().equals(DayOfWeek.SUNDAY) ? -1L : 0L;
		return result.plusWeeks(adjustment);
	}

	private static DayOfWeek parseDayOfWeek(int dayOfWeek) {
		dayOfWeek = Math.min(Math.max(dayOfWeek, 0), 7);
		dayOfWeek = dayOfWeek != 0 ? dayOfWeek : 7;
		return DayOfWeek.of(dayOfWeek);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Retrieves the nearest date before the source date that matches the specified date of the month.<br>
	 * If the specified date of the month exceeds the valid range, the closest valid value will be used by default.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;atPreviousDateOfMonth("2002-07-21", 1) -> [2002-07-01]
	 * &#9;atPreviousDateOfMonth("2002-07-21", 31) -> [2002-06-30]
	 * &#9;atPreviousDateOfMonth("2002-07-21", 21) -> [2002-06-21]</pre>
	 * @param source Source date object.
	 * @param dateOfMonth Target date of the month.
	 * @return Retrieved <b>LocalDate</b> value.
	 */
	public static LocalDate atPreviousDateOfMonth(Object source, int dateOfMonth) {
		return atDateOfMonthCore(source, dateOfMonth, -1);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Retrieves the nearest date after the source date that matches the specified date of the month.<br>
	 * If the specified date of the month exceeds the valid range, the closest valid value will be used by default.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;atNextDateOfMonth("2002-07-21", 1) -> [2002-08-01]
	 * &#9;atNextDateOfMonth("2002-07-21", 31) -> [2002-07-31]
	 * &#9;atNextDateOfMonth("2002-07-21", 21) -> [2002-08-21]</pre>
	 * @param source Source date object.
	 * @param dateOfMonth Target date of the month.
	 * @return Retrieved <b>LocalDate</b> value.
	 */
	public static LocalDate atNextDateOfMonth(Object source, int dateOfMonth) {
		return atDateOfMonthCore(source, dateOfMonth, 1);
	}

	private static LocalDate atDateOfMonthCore(Object source, int dateOfMonth, int offSet) {
		LocalDate result = atDateInMonth(source, dateOfMonth);
		if (result == null) {
			return null;
		}
		if (compareDate(source, result) != (-offSet)) {
			result = atDateInMonth(plusMonthsToDate(result, offSet), dateOfMonth);
		}
		return result;
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Retrieves the date in the same month as the source date that matches the specified date of the month.<br>
	 * If the specified date of the month exceeds the valid range, the closest valid value will be used by default.<br>
	 * Also supports <b>String</b> sources with only year month data.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;atDateInMonth("2002-07-21", 1) -> [2002-07-01]
	 * &#9;atDateInMonth("2002-02-01", 31) -> [2002-02-28]
	 * &#9;atDateInMonth("2002-07", 31) -> [2002-07-31]</pre>
	 * @param source Source date object.
	 * @param dateOfMonth Target date of month.
	 * @return Retrieved <b>LocalDate</b> value.
	 */
	public static LocalDate atDateInMonth(Object source, int dateOfMonth) {
		LocalDate result;
		if (source instanceof String) {
			String sourceString = (String) source;
			if (!EmbeddedStringUtil.isNullOrBlank(sourceString)) {
				for (Map.Entry<DateTimeFormatter, String> entry : PRESET_DATE_FORMAT.entrySet()) {
					result = parseDate(sourceString + entry.getValue(), entry.getKey());
					if (result != null) {
						return atDateInMonth(result, dateOfMonth);
					}
				}
			}
		}
		result = parseDate(source);
		if (result == null) {
			return null;
		}
		dateOfMonth = Math.min(Math.max(dateOfMonth, 1), result.lengthOfMonth());
		result = result.withDayOfMonth(1).plusDays(dateOfMonth - 1);
		return result;
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Retrieves the first date of the nearest month before the source month that matches the specified month.<br>
	 * If the specified month exceeds the valid range, the closest valid value will be used by default.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;atPreviousMonthOfYear("2002-07-21", 8) -> [2001-08-01]
	 * &#9;atPreviousMonthOfYear("2002-07-21", 7) -> [2001-07-01]
	 * &#9;atPreviousMonthOfYear("2002-07-21", 6) -> [2002-06-01]</pre>
	 * @param source Source date object.
	 * @param month Target month presented by an <b>int</b> value.
	 * @return Retrieved <b>LocalDate</b> value.
	 */
	public static LocalDate atPreviousMonthOfYear(Object source, int month) {
		return atMonthOfYearCore(source, month, -1);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Retrieves the first date of the nearest month before the source month that matches the specified month.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;atPreviousMonthOfYear("2002-07-21", {@link Month#AUGUST}) -> [2001-08-01]
	 * &#9;atPreviousMonthOfYear("2002-07-21", {@link Month#JULY}) -> [2001-07-01]
	 * &#9;atPreviousMonthOfYear("2002-07-21", {@link Month#JUNE}) -> [2002-06-01]</pre>
	 * @param source Source date object.
	 * @param month Target month presented by a {@link Month} emun.
	 * @return Retrieved <b>LocalDate</b> value.
	 */
	public static LocalDate atPreviousMonthOfYear(Object source, Month month) {
		return atMonthOfYearCore(source, month.getValue(), -1);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Retrieves the first date of the nearest month after the source month that matches the specified month.<br>
	 * If the specified month exceeds the valid range, the closest valid value will be used by default.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;atNextMonthOfYear("2002-07-21", 8) -> [2002-08-01]
	 * &#9;atNextMonthOfYear("2002-07-21", 7) -> [2003-07-01]
	 * &#9;atNextMonthOfYear("2002-07-21", 6) -> [2003-06-01]</pre>
	 * @param source Source date object.
	 * @param month Target month presented by an <b>int</b> value.
	 * @return Retrieved <b>LocalDate</b> value.
	 */
	public static LocalDate atNextMonthOfYear(Object source, int month) {
		return atMonthOfYearCore(source, month, 1);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Retrieves the first date of the nearest month after the source month that matches the specified month.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;atNextMonthOfYear("2002-07-21", {@link Month#AUGUST}) -> [2002-08-01]
	 * &#9;atNextMonthOfYear("2002-07-21", {@link Month#JULY}) -> [2003-07-01]
	 * &#9;atNextMonthOfYear("2002-07-21", {@link Month#JUNE}) -> [2003-06-01]</pre>
	 * @param source Source date object.
	 * @param month Target month presented by a {@link Month} emun.
	 * @return Retrieved <b>LocalDate</b> value.
	 */
	public static LocalDate atNextMonthOfYear(Object source, Month month) {
		return atMonthOfYearCore(source, month.getValue(), 1);
	}

	private static LocalDate atMonthOfYearCore(Object source, int month, int offSet) {
		LocalDate result = atFirstDateOfYear(source, month);
		if (result == null) {
			return null;
		}
		if (compareDate(source, result) != (-offSet)) {
			result = plusYearsToDate(result, offSet);
		}
		return result;
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Retrieves the first date of the same year as the source date.<br>
	 * Also supports <b>String</b> sources with only year month data or only year data.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;atFirstDateOfYear("2002-07-21") -> [2002-01-01]
	 * &#9;atFirstDateOfYear("2002-06") -> [2002-01-01]
	 * &#9;atFirstDateOfYear("2002") -> [2002-01-01]</pre>
	 * @param source Source date object.
	 * @return Retrieved <b>LocalDate</b> value.
	 */
	public static LocalDate atFirstDateOfYear(Object source) {
		return atFirstDateOfYear(source, 1);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Retrieves the first date of the same year as the source date assuming the specified month is the first month of the year.<br>
	 * If the specified month exceeds the valid range, the closest valid value will be used by default.<br>
	 * Also supports <b>String</b> sources with only year month data or only year data.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;atFirstDateOfYear("2002-07-21", 1) -> [2002-01-01]
	 * &#9;atFirstDateOfYear("2002-06", 7) -> [2001-07-01]
	 * &#9;atFirstDateOfYear("2002", 7) -> [2002-07-01]</pre>
	 * @param source Source date object.
	 * @param firstMonthAnnual The first month of a year presented by an <b>int</b> value.
	 * @return Retrieved <b>LocalDate</b> value.
	 */
	public static LocalDate atFirstDateOfYear(Object source, int firstMonthAnnual) {
		LocalDate result;
		if (source instanceof String) {
			String sourceString = (String) source;
			if (!EmbeddedStringUtil.isNullOrBlank(sourceString)) {
				result = parseDate(sourceString + YEAR_COMPLEMENT, DATE_FULL_PLAIN);
				if (result == null) {
					result = atDateInMonth(source, 1);
				}
				if (result != null) {
					return atFirstDateOfYear(result, firstMonthAnnual);
				}
			}
		}
		result = parseDate(source);
		if (result == null) {
			return null;
		}
		firstMonthAnnual = Math.min(Math.max(firstMonthAnnual, 1), 12);
		long adjustment = result.getMonthValue() < firstMonthAnnual ? -1L : 0L;
		return result.withMonth(firstMonthAnnual).withDayOfMonth(1).plusYears(adjustment);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Retrieves the first date of the same year as the source date assuming the specified month is the first month of the year.<br>
	 * Also supports <b>String</b> sources with only year month data or only year data.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;atFirstDateOfYear("2002-07-21", {@link Month#JANUARY}) -> [2002-01-01]
	 * &#9;atFirstDateOfYear("2002-06", {@link Month#JULY}) -> [2001-07-01]
	 * &#9;atFirstDateOfYear("2002", {@link Month#JULY}) -> [2002-07-01]</pre>
	 * @param source Source date object.
	 * @param firstMonthAnnual The first month of a year presented by a {@link Month} emun.
	 * @return Retrieved <b>LocalDate</b> value.
	 */
	public static LocalDate atFirstDateOfYear(Object source, Month firstMonthAnnual) {
		return atFirstDateOfYear(source, firstMonthAnnual.getValue());
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Retrieves the last date of the same year as the source date.<br>
	 * Also supports <b>String</b> sources with only year month data or only year data.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;atLastDateOfYear("2002-07-21") -> [2002-12-31]
	 * &#9;atLastDateOfYear("2002-06") -> [2002-12-31]
	 * &#9;atLastDateOfYear("2002") -> [2002-12-31]</pre>
	 * @param source Source date object.
	 * @return Retrieved <b>LocalDate</b> value.
	 */
	public static LocalDate atLastDateOfYear(Object source) {
		return atLastDateOfYear(source, 1);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Retrieves the last date of the same year as the source date assuming the specified month is the first month of the year.<br>
	 * If the specified month exceeds the valid range, the closest valid value will be used by default.<br>
	 * Also supports <b>String</b> sources with only year month data or only year data.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;atLastDateOfYear("2002-07-21", 1) -> [2001-12-31]
	 * &#9;atLastDateOfYear("2002-06", 7) -> [2002-06-30]
	 * &#9;atLastDateOfYear("2002", 7) -> [2003-06-30]</pre>
	 * @param source Source date object.
	 * @param firstMonthAnnual The first month of a year presented by an <b>int</b> value.
	 * @return Retrieved <b>LocalDate</b> value.
	 */
	public static LocalDate atLastDateOfYear(Object source, int firstMonthAnnual) {
		LocalDate result = atFirstDateOfYear(source, firstMonthAnnual);
		if (result == null) {
			return null;
		}
		return result.plusYears(1L).minusDays(1L);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Retrieves the last date of the same year as the source date assuming the specified month is the first month of the year.<br>
	 * Also supports <b>String</b> sources with only year month data or only year data.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;atLastDateOfYear("2002-07-21", {@link Month#JANUARY}) -> [2001-12-31]
	 * &#9;atLastDateOfYear("2002-06", {@link Month#JULY}) -> [2002-06-30]
	 * &#9;atLastDateOfYear("2002", {@link Month#JULY}) -> [2003-06-30]</pre>
	 * @param source Source date object.
	 * @param firstMonthAnnual The first month of a year presented by a {@link Month} enum.
	 * @return Retrieved <b>LocalDate</b> value.
	 */
	public static LocalDate atLastDateOfYear(Object source, Month firstMonthAnnual) {
		return atLastDateOfYear(source, firstMonthAnnual.getValue());
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Retrieves the date that is a certain number of days before or after the source date.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source Source date object.
	 * @param days Number of days.
	 * @return Retrieved <b>LocalDate</b> value.
	 */
	public static LocalDate plusDaysToDate(Object source, Integer days) {
		return plusToDateCore(source, days, ChronoUnit.DAYS);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Retrieves the date that is a certain number of weeks before or after the source date.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source Source date object.
	 * @param weeks Number of weeks.
	 * @return Retrieved <b>LocalDate</b> value.
	 */
	public static LocalDate plusWeeksToDate(Object source, Integer weeks) {
		return plusToDateCore(source, weeks, ChronoUnit.WEEKS);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Retrieves the date that is a certain number of months before or after the source date.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source Source date object.
	 * @param months Number of months.
	 * @return Retrieved <b>LocalDate</b> value.
	 */
	public static LocalDate plusMonthsToDate(Object source, Integer months) {
		return plusToDateCore(source, months, ChronoUnit.MONTHS);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Retrieves the date that is a certain number of years before or after the source date.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source Source date object.
	 * @param years Number of years.
	 * @return Retrieved <b>LocalDate</b> value.
	 */
	public static LocalDate plusYearsToDate(Object source, Integer years) {
		return plusToDateCore(source, years, ChronoUnit.YEARS);
	}

	private static LocalDate plusToDateCore(Object source, Integer spanValue, ChronoUnit unit) {
		LocalDate result = parseDate(source);
		if (result == null) {
			return null;
		}
		return result.plus(spanValue != null ? spanValue : 0L, unit);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Retrieves the date that is the specified duration before or after the source date.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source Source date object.
	 * @param duration Target duration span.
	 * @return Retrieved <b>LocalDate</b> value.
	 */
	public static LocalDate plusDurationToDate(Object source, Duration duration) {
		LocalDate result = parseDate(source);
		if (result == null || duration == null) {
			return result;
		}
		return result.plus(duration);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Gets the number of days between the two target dates.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source1 The first target date object.
	 * @param source2 The second target date object.
	 * @return Number of day span.
	 */
	public static int getDaySpanBetweenDate(Object source1, Object source2) {
		return getSpanBetweenDateCore(source1, source2, ChronoUnit.DAYS);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Gets the number of weeks between the two target dates.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source1 The first target date object.
	 * @param source2 The second target date object.
	 * @return Number of week span.
	 */
	public static int getWeekSpanBetweenDate(Object source1, Object source2) {
		return getSpanBetweenDateCore(source1, source2, ChronoUnit.WEEKS);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Gets the number of months between the two target dates.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source1 The first target date object.
	 * @param source2 The second target date object.
	 * @return Number of month span.
	 */
	public static int getMonthSpanBetweenDate(Object source1, Object source2) {
		return getSpanBetweenDateCore(source1, source2, ChronoUnit.MONTHS);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Gets the number of years between the two target dates.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source1 The first target date object.
	 * @param source2 The second target date object.
	 * @return Number of year span.
	 */
	public static int getYearSpanBetweenDate(Object source1, Object source2) {
		return getSpanBetweenDateCore(source1, source2, ChronoUnit.YEARS);
	}

	private static int getSpanBetweenDateCore(Object source1, Object source2, ChronoUnit unit) {
		LocalDate date1 = parseDate(source1), date2 = parseDate(source2);
		if (date1 == null || date2 == null) {
			return 0;
		}
		return (int) date1.until(date2, unit);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Gets the duration between the two target dates.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source1 The first target date object.
	 * @param source2 The second target date object.
	 * @return <b>Duration</b> date span.
	 */
	public static Duration getDurationBetweenDate(Object source1, Object source2) {
		LocalDate date1 = parseDate(source1), date2 = parseDate(source2);
		if (date1 == null || date2 == null) {
			return Duration.ZERO;
		}
		return Duration.between(date1, date2);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Fetches the latest date among the target dates.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param sources Target date objects.
	 * @return The latest <b>LocalDate</b> value.
	 */
	public static LocalDate maxDate(Object... sources) {
		LocalDate result = null;
		for (Object source : sources) {
			LocalDate candidate = parseDate(source);
			int compareResult = compareDate(candidate, result);
			if (compareResult == 1 || compareResult == 2) {
				result = candidate;
			}
		}
		return result;
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Fetches the earliest date among the target dates.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param sources Target date objects.
	 * @return The earliest <b>LocalDate</b> value.
	 */
	public static LocalDate minDate(Object... sources) {
		LocalDate result = null;
		for (Object source : sources) {
			LocalDate candidate = parseDate(source);
			int compareResult = compareDate(candidate, result);
			if (compareResult == -1 || compareResult == 2) {
				result = candidate;
			}
		}
		return result;
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Evaluates if the two target dates represent the same date.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param comparandObject1 Target date object to be compared.
	 * @param comparandObject2 Target date object to be compared.
	 * @return {@code true} if equal.
	 */
	public static boolean areSameDate(Object comparandObject1, Object comparandObject2) {
		return compareDate(comparandObject1, comparandObject2) == 0;
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Evaluates if the two target dates are in the same month.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param comparandObject1 Target date object to be compared.
	 * @param comparandObject2 Target date object to be compared.
	 * @return {@code true} if in.
	 */
	public static boolean areInSameMonth(Object comparandObject1, Object comparandObject2) {
		LocalDate comparand1 = parseDate(comparandObject1), comparand2 = parseDate(comparandObject2);
		if (comparand1 == null || comparand2 == null) {
			return false;
		}
		return comparand1.getYear() == comparand2.getYear() &&
				comparand1.getMonthValue() == comparand2.getMonthValue();
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Evaluates if the two target dates are in the same year.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param comparandObject1 Target date object to be compared.
	 * @param comparandObject2 Target date object to be compared.
	 * @return {@code true} if in.
	 */
	public static boolean areInSameYear(Object comparandObject1, Object comparandObject2) {
		LocalDate comparand1 = parseDate(comparandObject1), comparand2 = parseDate(comparandObject2);
		if (comparand1 == null || comparand2 == null) {
			return false;
		}
		return comparand1.getYear() == comparand2.getYear();
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Evaluates if the target dates are in order from the earliest to the latest.<br>
	 * Adjacent arguments with same value are considered in proper sequence, hence no effect to the evaluating result.<br>
	 * Any argument whose parse result is {@code null} will be ignored, hence no effect to the evaluating result.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;areInSequenceDate("2002-07-21", "2002-07-22", "2002-07-23") -> true
	 * &#9;areInSequenceDate("2002-07-22", "2002-07-21", "2002-07-23") -> false
	 * &#9;areInSequenceDate("2002-07-21", "2002-07-21", "2002-07-23") -> true
	 * &#9;areInSequenceDate("2002-07-21", "2002-07-40", "2002-07-23") -> true</pre>
	 * @param comparandObjects Target date objects to be compared.
	 * @return {@code true} if in proper sequence.
	 */
	public static boolean areInSequenceDate(Object... comparandObjects) {
		return areInSequenceDateCore(comparandObjects, SEQUENCE_INVALID_COMPARE_RESULT_PLAIN);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Evaluates if the target dates are in order from the earliest to the latest.<br>
	 * <font color="#EE2222"><b>Appearance of adjacent arguments with same value is considered to be violating the proper sequence.</b></font><br>
	 * Any argument whose parse result is {@code null} will be ignored, hence no effect to the evaluating result.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;areInSequenceDateNotEqual("2002-07-21", "2002-07-22", "2002-07-23") -> true
	 * &#9;areInSequenceDateNotEqual("2002-07-22", "2002-07-21", "2002-07-23") -> false
	 * &#9;areInSequenceDateNotEqual("2002-07-21", "2002-07-21", "2002-07-23") -> false
	 * &#9;areInSequenceDateNotEqual("2002-07-21", "2002-07-40", "2002-07-23") -> true</pre>
	 * @param comparandObjects Target date objects to be compared.
	 * @return {@code true} if in proper sequence.
	 */
	public static boolean areInSequenceDateNotEqual(Object... comparandObjects) {
		return areInSequenceDateCore(comparandObjects, SEQUENCE_INVALID_COMPARE_RESULT_NOT_EQUAL);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Evaluates if the target dates are in order from the earliest to the latest.<br>
	 * Adjacent arguments with same value are considered in proper sequence, hence no effect to the evaluating result.<br>
	 * <font color="#EE2222"><b>Any argument whose parse result is {@code null} is considered to be violating the proper sequence.</b></font><br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;areInSequenceDateNotNull("2002-07-21", "2002-07-22", "2002-07-23") -> true
	 * &#9;areInSequenceDateNotNull("2002-07-22", "2002-07-21", "2002-07-23") -> false
	 * &#9;areInSequenceDateNotNull("2002-07-21", "2002-07-21", "2002-07-23") -> true
	 * &#9;areInSequenceDateNotNull("2002-07-21", "2002-07-40", "2002-07-23") -> false</pre>
	 * @param comparandObjects Target date objects to be compared.
	 * @return {@code true} if in proper sequence.
	 */
	public static boolean areInSequenceDateNotNull(Object... comparandObjects) {
		return areInSequenceDateCore(comparandObjects, SEQUENCE_INVALID_COMPARE_RESULT_NOT_NULL);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Evaluates if the target dates are in order from the earliest to the latest.<br>
	 * <font color="#EE2222"><b>Appearance of adjacent arguments with same value is considered to be violating the proper sequence.</b></font><br>
	 * <font color="#EE2222"><b>Any argument whose parse result is {@code null} is considered to be violating the proper sequence.</b></font><br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;areInSequenceDateNotEqualNull("2002-07-21", "2002-07-22", "2002-07-23") -> true
	 * &#9;areInSequenceDateNotEqualNull("2002-07-22", "2002-07-21", "2002-07-23") -> false
	 * &#9;areInSequenceDateNotEqualNull("2002-07-21", "2002-07-21", "2002-07-23") -> false
	 * &#9;areInSequenceDateNotEqualNull("2002-07-21", "2002-07-40", "2002-07-23") -> false</pre>
	 * @param comparandObjects Target date objects to be compared.
	 * @return {@code true} if in proper sequence.
	 */
	public static boolean areInSequenceDateNotEqualNull(Object... comparandObjects) {
		return areInSequenceDateCore(comparandObjects, SEQUENCE_INVALID_COMPARE_RESULT_NOT_EQUAL_NULL);
	}

	private static boolean areInSequenceDateCore(Object[] comparandObjects, List<Integer> sequenceInvalidCompareResult) {
		return areInSequenceCore(comparandObjects, sequenceInvalidCompareResult, DateTimeUtil::compareDate, DateTimeUtil::isDate);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Evaluates size relationship between the two target dates.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param comparandObject1 The first target date object to be compared.
	 * @param comparandObject2 The second target date object to be compared.
	 * @return Comparison result.
	 * @see #compare(Object, Object)
	 */
	public static int compareDate(Object comparandObject1, Object comparandObject2) {
		return compare(parseDate(comparandObject1), parseDate(comparandObject2));
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Provides an ascending <b>Comparator&lt;Type&gt;</b> available in methods such as {@link List#sort(Comparator)}.<br>
	 * Uses {@link #compareDate(Object, Object)} as base method.
	 * <pre><b><i>Eg.:</i></b>&#9;userList.sort(DecimalUtil.compareDateAsc(User::getBirthDay))</pre>
	 * @param <Type> The type of object to be compared.
	 * @param fieldGetter The getter of the field to be used for comparing.
	 * @return <b>Comparator&lt;Type&gt;</b><br>
	 */
	public static <Type> Comparator<Type> compareDateAsc(Function<Type, Object> fieldGetter) {
		return (entity1, entity2) -> compareDate(fieldGetter.apply(entity1), fieldGetter.apply(entity2));
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Provides a descending <b>Comparator&lt;Type&gt;</b> available in methods such as {@link List#sort(Comparator)}.<br>
	 * Uses {@link #compare(Object, Object)} as base method.
	 * <pre><b><i>Eg.:</i></b>&#9;userList.sort(DecimalUtil.compareDateDesc(User::getBirthDay))</pre>
	 * @param <Type> The type of object to be compared.
	 * @param fieldGetter The getter of the field to be used for comparing.
	 * @return <b>Comparator&lt;Type&gt;</b><br>
	 */
	public static <Type> Comparator<Type> compareDateDesc(Function<Type, Object> fieldGetter) {
		return (entity1, entity2) -> compareDate(fieldGetter.apply(entity2), fieldGetter.apply(entity1));
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// date output

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Efficiently formats the current date with the preset format: <b><u>yyyyMMdd</u></b>, eg.: <b><u>20020721</u></b>.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String nowDate_fullPlain() {
		return nowDate(DATE_FULL_PLAIN);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Efficiently formats the current date with the preset format: <b><u>yyyy/MM/dd</u></b>, eg.: <b><u>2002/07/21</u></b>.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String nowDate_fullSlash() {
		return nowDate(DATE_FULL_SLASH);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Efficiently formats the current date with the preset format: <b><u>yyyy-MM-dd</u></b>, eg.: <b><u>2002-07-21</u></b>.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String nowDate_fullDash() {
		return nowDate(DATE_FULL_DASH);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Efficiently formats the current date with the preset format: <b><u>yyyy年MM月dd日</u></b>, eg.: <b><u>2002年07月21日</u></b>.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String nowDate_fullChar() {
		return nowDate(DATE_FULL_CHAR);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Efficiently formats the current date with the specified format.
	 * @param formatPattern Target date format presented by a <b>String</b> char sequence.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String nowDate(String formatPattern) {
		return nowDate(DateTimeFormatter.ofPattern(formatPattern));
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Efficiently formats the current date with the specified format.
	 * @param format Target date format presented by a {@link DateTimeFormatter} object.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String nowDate(DateTimeFormatter format) {
		return formatDate(LocalDate.now(), format);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Efficiently formats the target date with the preset format: <b><u>yyyyMMdd</u></b>, eg.: <b><u>20020721</u></b>.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source Target object to be parsed and formatted.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String formatDate_fullPlain(Object source) {
		return formatDate(source, DATE_FULL_PLAIN);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Efficiently formats the target date with the preset format: <b><u>yyyy/MM/dd</u></b>, eg.: <b><u>2002/07/21</u></b>.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source Target object to be parsed and formatted.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String formatDate_fullSlash(Object source) {
		return formatDate(source, DATE_FULL_SLASH);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Efficiently formats the target date with the preset format: <b><u>yyyy-MM-dd</u></b>, eg.: <b><u>2002-07-21</u></b>.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source Target object to be parsed and formatted.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String formatDate_fullDash(Object source) {
		return formatDate(source, DATE_FULL_DASH);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Efficiently formats the target date with the preset format: <b><u>yyyy年MM月dd日</u></b>, eg.: <b><u>2002年07月21日</u></b>.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source Target object to be parsed and formatted.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String formatDate_fullChar(Object source) {
		return formatDate(source, DATE_FULL_CHAR);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Efficiently formats the target date with the specified format.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source Target object to be parsed and formatted.
	 * @param formatPattern Target date format presented by a <b>String</b> char sequence.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String formatDate(Object source, String formatPattern) {
		return formatDate(source, DateTimeFormatter.ofPattern(formatPattern));
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Efficiently formats the target date with the specified format.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source Target object to be parsed and formatted.
	 * @param format Target date format presented by a {@link DateTimeFormatter} object.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String formatDate(Object source, DateTimeFormatter format) {
		LocalDate result = parseDate(source);
		if (result == null) {
			return null;
		}
		return result.format(format);
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Efficiently formats the target date with the format of historical chronology of Japan.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source Target object to be parsed and formatted.
	 * @return Formatted <b>String</b> char sequence.
	 * @see #JP_ERA_NAME
	 */
	public static String formatDate_jp(Object source) {
		LocalDate result = parseDate(source);
		if (result == null) {
			return null;
		}
		String monthDay = formatDate(result, "M月d日");
		int year = result.getYear();
		String yearResult = year + "年";
		for (Map.Entry<String, EraYearSpan> entry : JP_ERA_NAME.entrySet()) {
			if (year >= entry.getValue().getBegin() && year <= entry.getValue().getEnd()) {
				int eraYear = year - entry.getValue().getBegin() + 1;
				yearResult = entry.getKey() + (eraYear == 1 ? "元" : eraYear) + "年";
				break;
			}
		}
		return yearResult + monthDay;
	}

	/**
	 * <font color="#2222EE"><b>Date operation.</b></font><br>
	 * Efficiently gets the target day of the week with the Japanese format.<br>
	 * @param source Target date object.
	 * @return Day of the week <b>String</b> char sequence.
	 * @see #JP_DAY_OF_WEEK_NAME
	 */
	public static String formatDayOfWeek_jp(Object source) {
		for (Map.Entry<String, List<Object>> entry : JP_DAY_OF_WEEK_NAME.entrySet()) {
			if (entry.getValue().contains(source)) {
				return entry.getKey();
			}
		}
		LocalDate date = parseDate(source);
		if (date != null) {
			DayOfWeek dayOfWeek = date.getDayOfWeek();
			for (Map.Entry<String, List<Object>> entry : JP_DAY_OF_WEEK_NAME.entrySet()) {
				if (entry.getValue().contains(dayOfWeek)) {
					return entry.getKey();
				}
			}
		}
		return null;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// time getter

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Parses an instance of a supported type into a <b>LocalTime</b> object.<br>
	 * Supports instances of: <b>LocalDateTime</b>, <b>LocalTime</b>, <b>Timestamp</b>, <b>String</b>,
	 * <b>Long(long)</b>, <b>Integer(int)</b>.<br>
	 * <b>String</b> arguments will be parsed with one of the preset formats,
	 * and will return {@code null} if fails to parse using any of those formats.<br>
	 * <b>Long(long)</b> arguments will be recognized same as <b>Timestamp</b>,
	 * while <b>Integer(int)</b> same as <b>String</b>.<br>
	 * Passing {@code null} will return {@code null}.<br>
	 * Passing an unsupported arguments will throw a <b>DateTimeParseException</b>.
	 * @param source Target object to be parsed into <b>LocalTime</b>.
	 * @return Parsed <b>LocalTime</b> value.
	 */
	public static LocalTime parseTime(Object source) {
		LocalTime result = null;
		if (source == null) {
			return null;
		} else if (source instanceof String) {
			String sourceString = (String) source;
			if (!EmbeddedStringUtil.isNullOrBlank(sourceString)) {
				for (DateTimeFormatter format : PRESET_TIME_FORMAT.keySet()) {
					result = parseTime(sourceString, format);
					if (result != null) {
						break;
					}
				}
			}
		} else if (source instanceof LocalTime) {
			result = LocalTime.from((LocalTime) source);
		} else if (source instanceof LocalDateTime) {
			result = ((LocalDateTime) source).toLocalTime();
		} else if (source instanceof Timestamp) {
			result = ((Timestamp) source).toLocalDateTime().toLocalTime();
		} else if (source instanceof Long) {
			result = parseTime(new Timestamp((Long) source));
		} else if (source instanceof Integer) {
			StringBuilder sourceString = new StringBuilder(((Integer) source).toString());
			for (int index = sourceString.length(); index < 6; index ++) {
				sourceString.insert(0, 0);
			}
			result = parseTime(sourceString.toString());
		} else {
			throw new DateTimeParseException("Unparseable argument(s) passed in", source.toString(), 0);
		}
		return result;
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Parses a <b>String</b> char sequence into a <b>LocalTime</b> object with the specified format.<br>
	 * Returns {@code null} if an empty char sequence is passed in or fails to parse using the specified format.
	 * @param source Target <b>String</b> char sequence to be parsed into <b>LocalTime</b>.
	 * @param formatPattern Target time format presented by a <b>String</b> char sequence.
	 * @return Parsed <b>LocalTime</b> value.
	 */
	public static LocalTime parseTime(String source, String formatPattern) {
		return parseTime(source, DateTimeFormatter.ofPattern(formatPattern));
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Parses a <b>String</b> char sequence into a <b>LocalTime</b> object with the specified format.<br>
	 * Returns {@code null} if an empty char sequence is passed in or fails to parse using the specified format.
	 * @param source Target <b>String</b> char sequence to be parsed into <b>LocalTime</b>.
	 * @param format Target time format presented by a {@link DateTimeFormatter} object.
	 * @return Parsed <b>LocalTime</b> value.
	 */
	public static LocalTime parseTime(String source, DateTimeFormatter format) {
		if (EmbeddedStringUtil.isNullOrBlank(source)) {
			return null;
		}
		try {
			return LocalTime.parse(source, format);
		} catch (DateTimeParseException exception) {
			return null;
		}
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Evaluates if the target object can be parsed into a valid <b>LocalTime</b> object.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source Target time object.
	 * @return {@code true} if successfully parsed.
	 */
	public static boolean isTime(Object source) {
		return parseTime(source) != null;
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Evaluates if the target char sequence can be parsed into a valid <b>LocalTime</b> object with the specified format.
	 * @param source Target <b>String</b> char sequence.
	 * @param formatPattern Target time format presented by a <b>String</b> char sequence.
	 * @return {@code true} if successfully parsed.
	 */
	public static boolean isTime(String source, String formatPattern) {
		return parseTime(source, formatPattern) != null;
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Evaluates if the target char sequence can be parsed into a valid <b>LocalTime</b> object with the specified format.
	 * @param source Target <b>String</b> char sequence.
	 * @param format Target time format presented by a {@link DateTimeFormatter} object.
	 * @return {@code true} if successfully parsed.
	 */
	public static boolean isTime(String source, DateTimeFormatter format) {
		return parseTime(source, format) != null;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// time calculating

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Retrieves the time that is right at the start of the source time's minute.<br>
	 * Also supports <b>String</b> sources with only hour minute data.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;atStartOfMinute("12:50:31") -> [12:50:00]
	 * &#9;atStartOfMinute("12:50") -> [12:50:00]</pre>
	 * @param source Source time object.
	 * @return Retrieved <b>LocalTime</b> value.
	 */
	public static LocalTime atStartOfMinute(Object source) {
		LocalTime result;
		if (source instanceof String) {
			String sourceString = (String) source;
			if (!EmbeddedStringUtil.isNullOrBlank(sourceString)) {
				for (Map.Entry<DateTimeFormatter, String> entry : PRESET_TIME_FORMAT.entrySet()) {
					result = parseTime(sourceString + entry.getValue(), entry.getKey());
					if (result != null) {
						return atStartOfMinute(result);
					}
				}
			}
		}
		result = parseTime(source);
		if (result == null) {
			return null;
		}
		return result.withSecond(0).withNano(0);
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Retrieves the time that is right at the start of the source time's hour.<br>
	 * Also supports <b>String</b> sources with only hour minute data or only hour data.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;atStartOfHour("12:50:31") -> [12:00:00]
	 * &#9;atStartOfHour("12:50") -> [12:00:00]
	 * &#9;atStartOfHour("12") -> [12:00:00]</pre>
	 * @param source Source time object.
	 * @return Retrieved <b>LocalTime</b> value.
	 */
	public static LocalTime atStartOfHour(Object source) {
		LocalTime result;
		if (source instanceof String) {
			String sourceString = (String) source;
			if (!EmbeddedStringUtil.isNullOrBlank(sourceString)) {
				result = parseTime(sourceString + HOUR_COMPLEMENT, TIME_BASIC_PLAIN);
				if (result == null) {
					result = atStartOfMinute(source);
				}
				if (result != null) {
					return atStartOfHour(result);
				}
			}
			return null;
		}
		result = parseTime(source);
		if (result == null) {
			return null;
		}
		return result.withMinute(0).withSecond(0).withNano(0);
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Retrieves the time that is right at the midpoint of the source time's hour.<br>
	 * Also support <b>String</b> source with only hour minute data or only hour data.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;atHalfOfHour("12:50:31") -> [12:30:00]
	 * &#9;atHalfOfHour("12:50") -> [12:30:00]
	 * &#9;atHalfOfHour("12") -> [12:30:00]</pre>
	 * @param source Source time object.
	 * @return Retrieved <b>LocalTime</b> value.
	 */
	public static LocalTime atHalfOfHour(Object source) {
		LocalTime result = atStartOfHour(source);
		if (result == null) {
			return null;
		}
		return result.plusMinutes(30L);
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Retrieves the time that is a certain number of seconds before or after the source time.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source Source time object.
	 * @param seconds Number of seconds
	 * @return Retrieved <b>LocalTime</b> value.
	 */
	public static LocalTime plusSecondsToTime(Object source, Integer seconds) {
		return plusToTimeCore(source, seconds, ChronoUnit.SECONDS);
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Retrieves the time that is a certain number of minutes before or after the source time.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source Source time object.
	 * @param minutes Number of minutes.
	 * @return Retrieved <b>LocalTime</b> value.
	 */
	public static LocalTime plusMinutesToTime(Object source, Integer minutes) {
		return plusToTimeCore(source, minutes, ChronoUnit.MINUTES);
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Retrieves the time that is a certain number of hours before or after the source time.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source Source time object.
	 * @param hours Number of hours.
	 * @return Retrieved <b>LocalTime</b> value.
	 */
	public static LocalTime plusHoursToTime(Object source, Integer hours) {
		return plusToTimeCore(source, hours, ChronoUnit.HOURS);
	}

	private static LocalTime plusToTimeCore(Object source, Integer spanValue, ChronoUnit unit) {
		LocalTime result = parseTime(source);
		if (result == null) {
			return null;
		}
		return result.plus(spanValue != null ? spanValue : 0L, unit);
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Retrieves the time that is the specified duration before or after the source time.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source Source time object.
	 * @param duration Target duration span.
	 * @return Retrieved <b>LocalTime</b> value.
	 */
	public static LocalTime plusDurationToTime(Object source, Duration duration) {
		LocalTime result = parseTime(source);
		if (result == null || duration == null) {
			return result;
		}
		return result.plus(duration);
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Gets the number of seconds between the two target times.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source1 The first target time object.
	 * @param source2 The second target time object.
	 * @return Number of second span.
	 */
	public static int getSecondSpanBetweenTime(Object source1, Object source2) {
		return getSpanBetweenTimeCore(source1, source2, ChronoUnit.SECONDS);
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Gets the number of minutes between the two target times.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source1 The first target time object.
	 * @param source2 The second target time object.
	 * @return Number of minute span.
	 */
	public static int getMinuteSpanBetweenTime(Object source1, Object source2) {
		return getSpanBetweenTimeCore(source1, source2, ChronoUnit.MINUTES);
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Gets the number of hours between the two target times.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source1 The first target time object.
	 * @param source2 The second target time object.
	 * @return Number of hour span.
	 */
	public static int getHourSpanBetweenTime(Object source1, Object source2) {
		return getSpanBetweenTimeCore(source1, source2, ChronoUnit.HOURS);
	}

	private static int getSpanBetweenTimeCore(Object source1, Object source2, ChronoUnit unit) {
		LocalTime time1 = parseTime(source1), time2 = parseTime(source2);
		if (time1 == null || time2 == null) {
			return 0;
		}
		return (int) time1.until(time2, unit);
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Gets the duration between the two target times.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source1 The first target time object.
	 * @param source2 The second target time object.
	 * @return <b>Duration</b> time span.
	 */
	public static Duration getDurationBetweenTime(Object source1, Object source2) {
		LocalTime time1 = parseTime(source1), time2 = parseTime(source2);
		if (time1 == null || time2 == null) {
			return Duration.ZERO;
		}
		return Duration.between(time1, time2);
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Fetches the latest time among the target times.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param sources Target time objects.
	 * @return The latest <b>LocalTime</b> value.
	 */
	public static LocalTime maxTime(Object... sources) {
		LocalTime result = null;
		for (Object source : sources) {
			LocalTime candidate = parseTime(source);
			int compareResult = compareTime(candidate, result);
			if (compareResult == 1 || compareResult == 2) {
				result = candidate;
			}
		}
		return result;
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Fetches the earliest time among the target times.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param sources Target time objects.
	 * @return The latest <b>LocalTime</b> value.
	 */
	public static LocalTime minTime(Object... sources) {
		LocalTime result = null;
		for (Object source : sources) {
			LocalTime candidate = parseTime(source);
			int compareResult = compareTime(candidate, result);
			if (compareResult == -1 || compareResult == 2) {
				result = candidate;
			}
		}
		return result;
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Evaluates if the two target times represent the same second.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param comparandObject1 Target time object to be compared.
	 * @param comparandObject2 Target time object to be compared.
	 * @return {@code true} if equal.
	 */
	public static boolean areInSameSecond(Object comparandObject1, Object comparandObject2) {
		LocalTime comparand1 = parseTime(comparandObject1), comparand2 = parseTime(comparandObject2);
		if (comparand1 == null || comparand2 == null) {
			return false;
		}
		return comparand1.getHour() == comparand2.getHour() &&
				comparand1.getMinute() == comparand2.getMinute() &&
				comparand1.getSecond() == comparand2.getSecond();
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Evaluates if the two target times are in the same minute.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param comparandObject1 Target time object to be compared.
	 * @param comparandObject2 Target time object to be compared.
	 * @return {@code true} if in.
	 */
	public static boolean areInSameMinute(Object comparandObject1, Object comparandObject2) {
		LocalTime comparand1 = parseTime(comparandObject1), comparand2 = parseTime(comparandObject2);
		if (comparand1 == null || comparand2 == null) {
			return false;
		}
		return comparand1.getHour() == comparand2.getHour() &&
				comparand1.getMinute() == comparand2.getMinute();
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Evaluates if the two target times are in the same hour.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param comparandObject1 Target time object to be compared.
	 * @param comparandObject2 Target time object to be compared.
	 * @return {@code true} if in.
	 */
	public static boolean areInSameHour(Object comparandObject1, Object comparandObject2) {
		LocalTime comparand1 = parseTime(comparandObject1), comparand2 = parseTime(comparandObject2);
		if (comparand1 == null || comparand2 == null) {
			return false;
		}
		return comparand1.getHour() == comparand2.getHour();
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Evaluates if the target times are in order from the earliest to the latest.<br>
	 * Adjacent arguments with same value are considered in proper sequence, hence no effect to the evaluating result.<br>
	 * Any argument whose parse result is {@code null} will be ignored, hence no effect to the evaluating result.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;areInSequenceTime("12:30:10", "12:30:20", "12:30:30") -> true
	 * &#9;areInSequenceTime("12:30:20", "12:30:10", "12:30:30") -> false
	 * &#9;areInSequenceTime("12:30:10", "12:30:10", "12:30:30") -> true
	 * &#9;areInSequenceTime("12:30:10", "12:30:80", "12:30:30") -> true</pre>
	 * @param comparandObjects Target time objects to be compared.
	 * @return {@code true} if in proper sequence.
	 */
	public static boolean areInSequenceTime(Object... comparandObjects) {
		return areInSequenceTimeCore(comparandObjects, SEQUENCE_INVALID_COMPARE_RESULT_PLAIN);
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Check if the target times are in order from smallest to largest.<br>
	 * <font color="#EE2222"><b>Appearance of adjacent arguments with same value is considered to be violating the proper sequence.</b></font><br>
	 * Any argument whose parsing result is {@code null} will be ignored, hence no affect to the checking result.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;areInSequenceTimeNotEqual("12:30:10", "12:30:20", "12:30:30") -> true
	 * &#9;areInSequenceTimeNotEqual("12:30:20", "12:30:10", "12:30:30") -> false
	 * &#9;areInSequenceTimeNotEqual("12:30:10", "12:30:10", "12:30:30") -> false
	 * &#9;areInSequenceTimeNotEqual("12:30:10", "12:30:80", "12:30:30") -> true</pre>
	 * @param comparandObjects Target time objects to be compared.
	 * @return {@code true} if in proper sequence.
	 */
	public static boolean areInSequenceTimeNotEqual(Object... comparandObjects) {
		return areInSequenceTimeCore(comparandObjects, SEQUENCE_INVALID_COMPARE_RESULT_NOT_EQUAL);
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Check if the target times are in order from smallest to largest.<br>
	 * Adjacent arguments with same value are considered in proper sequence, hence no effect to the evaluating result.<br>
	 * <font color="#EE2222"><b>Any argument whose parse result is {@code null} is considered to be violating the proper sequence.</b></font><br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;areInSequenceTimeNotNull("12:30:10", "12:30:20", "12:30:30") -> true
	 * &#9;areInSequenceTimeNotNull("12:30:20", "12:30:10", "12:30:30") -> false
	 * &#9;areInSequenceTimeNotNull("12:30:10", "12:30:10", "12:30:30") -> true
	 * &#9;areInSequenceTimeNotNull("12:30:10", "12:30:80", "12:30:30") -> false</pre>
	 * @param comparandObjects Target time objects to be compared.
	 * @return {@code true} if in proper sequence.
	 */
	public static boolean areInSequenceTimeNotNull(Object... comparandObjects) {
		return areInSequenceTimeCore(comparandObjects, SEQUENCE_INVALID_COMPARE_RESULT_NOT_NULL);
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Evaluates if the target times are in order from the earliest to the latest.<br>
	 * <font color="#EE2222"><b>Appearance of adjacent arguments with same value is considered to be violating the proper sequence.</b></font><br>
	 * <font color="#EE2222"><b>Any argument whose parse result is {@code null} is considered to be violating the proper sequence.</b></font><br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;areInSequenceTimeNotEqualNull("12:30:10", "12:30:20", "12:30:30") -> true
	 * &#9;areInSequenceTimeNotEqualNull("12:30:20", "12:30:10", "12:30:30") -> false
	 * &#9;areInSequenceTimeNotEqualNull("12:30:10", "12:30:10", "12:30:30") -> false
	 * &#9;areInSequenceTimeNotEqualNull("12:30:10", "12:30:80", "12:30:30") -> false</pre>
	 * @param comparandObjects Target time objects to be compared.
	 * @return {@code true} if in proper sequence.
	 */
	public static boolean areInSequenceTimeNotEqualNull(Object... comparandObjects) {
		return areInSequenceTimeCore(comparandObjects, SEQUENCE_INVALID_COMPARE_RESULT_NOT_EQUAL_NULL);
	}

	private static boolean areInSequenceTimeCore(Object[] comparandObjects, List<Integer> sequenceInvalidCompareResult) {
		return areInSequenceCore(comparandObjects, sequenceInvalidCompareResult, DateTimeUtil::compareTime, DateTimeUtil::isTime);
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Evaluates size relationship between the two target times.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param comparandObject1 The first target time object to be compared.
	 * @param comparandObject2 The second target time object to be compared.
	 * @return Comparison result.
	 * @see #compare(Object, Object)
	 */
	public static int compareTime(Object comparandObject1, Object comparandObject2) {
		return compare(parseTime(comparandObject1), parseTime(comparandObject2));
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Provides an ascending <b>Comparator&lt;Type&gt;</b> available in methods such as {@link List#sort(Comparator)}.<br>
	 * Uses {@link #compareTime(Object, Object)} as base method.
	 * <pre><b><i>Eg.:</i></b>&#9;lessonList.sort(DecimalUtil.compareTimeAsc(Lesson::getBeginTime))</pre>
	 * @param <Type> The type of object to be compared.
	 * @param fieldGetter The getter of the field to be used for comparing.
	 * @return <b>Comparator&lt;Type&gt;</b><br>
	 */
	public static <Type> Comparator<Type> compareTimeAsc(Function<Type, Object> fieldGetter) {
		return (entity1, entity2) -> compareTime(fieldGetter.apply(entity1), fieldGetter.apply(entity2));
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Provides a descending <b>Comparator&lt;Type&gt;</b> available in methods such as {@link List#sort(Comparator)}.<br>
	 * Uses {@link #compare(Object, Object)} as base method.
	 * <pre><b><i>Eg.:</i></b>&#9;lessonList.sort(DecimalUtil.compareTimeDesc(Lesson::getBeginTime))</pre>
	 * @param <Type> The type of object to be compared.
	 * @param fieldGetter The getter of the field to be used for comparing.
	 * @return <b>Comparator&lt;Type&gt;</b><br>
	 */
	public static <Type> Comparator<Type> compareTimeDesc(Function<Type, Object> fieldGetter) {
		return (entity1, entity2) -> compareTime(fieldGetter.apply(entity2), fieldGetter.apply(entity1));
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// time output

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Efficiently formats the current time with the preset format: <b><u>HHmm</u></b>, eg.: <b><u>1230</u></b>.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String nowTime_shortPlain() {
		return nowTime(TIME_SHORT_PLAIN);
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Efficiently formats the current time with the preset format: <b><u>HH:mm</u></b>, eg.: <b><u>12:30</u></b>.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String nowTime_shortColon() {
		return nowTime(TIME_SHORT_COLON);
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Efficiently formats the current time with the preset format: <b><u>HHmmss</u></b>, eg.: <b><u>123050</u></b>.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String nowTime_basicPlain() {
		return nowTime(TIME_BASIC_PLAIN);
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Efficiently formats the current time with the preset format: <b><u>HH:mm:ss</u></b>, eg.: <b><u>12:30:50</u></b>.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String nowTime_basicColon() {
		return nowTime(TIME_BASIC_COLON);
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Efficiently formats the current time with the preset format: <b><u>HHmmssSSS</u></b>, eg.: <b><u>123050000</u></b>.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String nowTime_fullPlain() {
		return nowTime(TIME_FULL_PLAIN);
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Efficiently formats the current time with the preset format: <b><u>HH:mm:ss.SSS</u></b>, eg.: <b><u>12:30:05.000</u></b>.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String nowTime_fullColon() {
		return nowTime(TIME_FULL_COLON);
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Efficiently formats the current time with the specified format.
	 * @param formatPattern Target time format presented by a <b>String</b> char sequence.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String nowTime(String formatPattern) {
		return nowTime(DateTimeFormatter.ofPattern(formatPattern));
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Efficiently formats the current time with the specified format.
	 * @param format Target time format presented by a {@link DateTimeFormatter} object.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String nowTime(DateTimeFormatter format) {
		return formatTime(LocalTime.now(), format);
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Efficiently formats the target time with the preset format: <b><u>HHmm</u></b>, eg.: <b><u>1230</u></b>.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source Target object to be parsed and formatted.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String formatTime_shortPlain(Object source) {
		return formatTime(source, TIME_SHORT_PLAIN);
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Efficiently formats the target time with the preset format: <b><u>HH:mm</u></b>, eg.: <b><u>12:30</u></b>.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source Target object to be parsed and formatted.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String formatTime_shortColon(Object source) {
		return formatTime(source, TIME_SHORT_COLON);
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Efficiently formats the target time with the preset format: <b><u>HHmmss</u></b>, eg.: <b><u>123050</u></b>.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source Target object to be parsed and formatted.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String formatTime_basicPlain(Object source) {
		return formatTime(source, TIME_BASIC_PLAIN);
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Efficiently formats the target time with the preset format: <b><u>HH:mm:ss</u></b>, eg.: <b><u>12:30:50</u></b>.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source Target object to be parsed and formatted.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String formatTime_basicColon(Object source) {
		return formatTime(source, TIME_BASIC_COLON);
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Efficiently formats the target time with the preset format: <b><u>HHmmssSSS</u></b>, eg.: <b><u>123050000</u></b>.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source Target object to be parsed and formatted.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String formatTime_fullPlain(Object source) {
		return formatTime(source, TIME_FULL_PLAIN);
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Efficiently formats the target time with the preset format: <b><u>HH:mm:ss.SSS</u></b>, eg.: <b><u>12:30:05.000</u></b>.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source Target object to be parsed and formatted.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String formatTime_fullColon(Object source) {
		return formatTime(source, TIME_FULL_COLON);
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Efficiently formats the target time with the specified format.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source Target object to be parsed and formatted.
	 * @param formatPattern Target time format presented by a <b>String</b> char sequence.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String formatTime(Object source, String formatPattern) {
		return formatTime(source, DateTimeFormatter.ofPattern(formatPattern));
	}

	/**
	 * <font color="#EE2222"><b>Time operation.</b></font><br>
	 * Efficiently formats the target time with the specified format.<br>
	 * Uses {@link #parseDate(Object)} for automatic parsing.
	 * @param source Target object to be parsed and formatted.
	 * @param format Target time format presented by a {@link DateTimeFormatter} object.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String formatTime(Object source, DateTimeFormatter format) {
		LocalTime result = parseTime(source);
		if (result == null) {
			return null;
		}
		return result.format(format);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// datetime getter

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Parses an instance of a supported type into a <b>LocalDateTime</b> object.<br>
	 * Supports instances of: <b>LocalDateTime</b>, <b>LocalDate</b>, <b>LocalTime</b>, <b>Timestamp</b>,
	 * <b>Date</b>, <b>Calendar</b>, <b>String</b>, <b>Long(long)</b>, <b>Integer(int)</b>.<br>
	 * <b>String</b> arguments will be parsed with one of the preset formats,
	 * and will return {@code null} if fails to parse using any of those formats.<br>
	 * <b>Long(long)</b> arguments will be recognized same as <b>Timestamp</b>.<br>
	 * <b>Integer(int)</b> arguments will be recognized same as <b>String</b> and parsed as a date source first.
	 * If its parse result is not a valid <b>LocalDate</b>, then try as a time source.<br>
	 * Regardless of the source argument's type, the time will default to [00:00:00] if the source contains only date data,
	 * or to the current date if there is only time data in the source.<br>
	 * Passing {@code null} will return {@code null}.<br>
	 * Passing an unsupported argument will throw a <b>DateTimeParseException</b>.<br>
	 * @param source Target object to be parsed into <b>LocalDateTime</b>.
	 * @return Parsed <b>LocalDateTime</b> value.
	 */
	public static LocalDateTime parse(Object source) {
		LocalDateTime result = null;
		if (source == null) {
			return null;
		} else if (source instanceof String) {
			String sourceString = (String) source;
			if (!EmbeddedStringUtil.isNullOrBlank(sourceString)) {
				for (DateTimeFormatter format : PRESET_DATE_TIME_FORMAT.keySet()) {
					result = parse(sourceString, format);
					if (result != null) {
						break;
					}
				}
				if (result == null) {
					LocalDate date = parseDate(sourceString);
					if (date != null) {
						result = parse(date);
					}
				}
				if (result == null) {
					LocalTime time = parseTime(sourceString);
					if (time != null) {
						result = parse(time);
					}
				}
			}
		} else if (source instanceof LocalDateTime) {
			result = LocalDateTime.from((LocalDateTime) source);
		} else if (source instanceof LocalDate) {
			result = ((LocalDate) source).atStartOfDay();
		} else if (source instanceof LocalTime) {
			result = ((LocalTime) source).atDate(LocalDate.now());
		} else if (source instanceof Timestamp) {
			result = ((Timestamp) source).toLocalDateTime();
		} else if (source instanceof Date) {
			result = ((Date) source).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
		} else if (source instanceof Calendar) {
			result = parse(((Calendar) source).getTime());
		} else if (source instanceof Long) {
			result = parse(new Timestamp((Long) source));
		} else if (source instanceof Integer) {
			LocalDate date = parseDate(source);
			if (date != null) {
				result = parse(date);
			} else {
				LocalTime time = parseTime(source);
				if (time != null) {
					result = parse(time);
				}
			}
		} else {
			throw new DateTimeParseException("Unparseable argument(s) passed in", source.toString(), 0);
		}
		return result;
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Parses a <b>String</b> char sequence into a <b>LocalDateTime</b> object with the specified format.<br>
	 * Returns {@code null} if an empty char sequence is passed in or fails to parse using the specified format.
	 * @param source Target <b>String</b> char sequence to be parsed into <b>LocalDateTime</b>.
	 * @param formatPattern Target date time format presented by a <b>String</b> char sequence.
	 * @return Parsed <b>LocalDateTime</b> value.
	 */
	public static LocalDateTime parse(String source, String formatPattern) {
		return parse(source, DateTimeFormatter.ofPattern(formatPattern));
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Parses a <b>String</b> char sequence into a <b>LocalDateTime</b> object with the specified format.<br>
	 * Returns {@code null} if an empty char sequence is passed in or fails to parse using the specified format.
	 * @param source Target <b>String</b> char sequence to be parsed into <b>LocalDateTime</b>.
	 * @param format Target date time format presented by a {@link DateTimeFormatter} object.
	 * @return Parsed <b>LocalDateTime</b> value.
	 */
	public static LocalDateTime parse(String source, DateTimeFormatter format) {
		if (EmbeddedStringUtil.isNullOrBlank(source)) {
			return null;
		}
		try {
			return LocalDateTime.parse(source, format);
		} catch (DateTimeParseException exception) {
			return null;
		}
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Merges the first source's date and the second source's time to a new <b>LocalDateTime</b> object.<br>
	 * Uses the current date by default if there is no date data in the first source, and [00:00:00] the second.<br>
	 * Returns {@code null} if both sources contain no corresponding data.
	 * @param dateSource Target object to extract date data.
	 * @param timeSource Target object to extract time data.
	 * @return Merged <b>LocalDateTime</b> value.
	 */
	public static LocalDateTime mergeDateTime(Object dateSource, Object timeSource) {
		LocalDate date = parseDate(dateSource);
		LocalTime time = parseTime(timeSource);
		if (date == null && time == null) {
			return null;
		}
		if (date == null) {
			date = LocalDate.now();
		}
		if (time == null) {
			time = LocalTime.MIDNIGHT;
		}
		return LocalDateTime.of(date, time);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Evaluates if the target object can ben parsed into a valid <b>LocalDateTime</b> object.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source Target date time object.
	 * @return {@code true} if successfully parsed.
	 */
	public static boolean isDateTime(Object source) {
		return parse(source) != null;
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Evaluates if the target char sequence can ben parsed into a valid <b>LocalDateTime</b> object with the specified format.
	 * @param source Target <b>String</b> char sequence.
	 * @param formatPattern Target date time format presented by a <b>String</b> char sequence.
	 * @return {@code true} if successfully parsed.
	 */
	public static boolean isDateTime(String source, String formatPattern) {
		return parse(source, formatPattern) != null;
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Evaluates if the target char sequence can ben parsed into a valid <b>LocalDateTime</b> object with the specified format.
	 * @param source Target <b>String</b> char sequence.
	 * @param format Target date time format presented by a {@link DateTimeFormatter} object.
	 * @return {@code true} if successfully parsed.
	 */
	public static boolean isDateTime(String source, DateTimeFormatter format) {
		return parse(source, format) != null;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// datetime calculating

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Retrieves the date time that is a certain number of seconds before or after the source time.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source Source date time object.
	 * @param seconds Number of seconds.
	 * @return Retrieved <b>LocalDateTime</b> value.
	 */
	public static LocalDateTime plusSeconds(Object source, Integer seconds) {
		return plusCore(source, seconds, ChronoUnit.SECONDS);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Retrieves the date time that is a certain number of minutes before or after the source time.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source Source date time object.
	 * @param minutes Number of minutes.
	 * @return Retrieved <b>LocalDateTime</b> value.
	 */
	public static LocalDateTime plusMinutes(Object source, Integer minutes) {
		return plusCore(source, minutes, ChronoUnit.MINUTES);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Retrieves the date time that is a certain number of hours before or after the source time.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source Source date time object.
	 * @param hours Number of hours.
	 * @return Retrieved <b>LocalDateTime</b> value.
	 */
	public static LocalDateTime plusHours(Object source, Integer hours) {
		return plusCore(source, hours, ChronoUnit.HOURS);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Retrieves the date time that is a certain number of days before or after the source time.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source Source date time object.
	 * @param days Number of days.
	 * @return Retrieved <b>LocalDateTime</b> value.
	 */
	public static LocalDateTime plusDays(Object source, Integer days) {
		return plusCore(source, days, ChronoUnit.DAYS);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Retrieves the date time that is a certain number of weeks before or after the source time.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source Source date time object.
	 * @param weeks Number of weeks.
	 * @return Retrieved <b>LocalDateTime</b> value.
	 */
	public static LocalDateTime plusWeeks(Object source, Integer weeks) {
		return plusCore(source, weeks, ChronoUnit.WEEKS);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Retrieves the date time that is a certain number of months before or after the source time.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source Source date time object.
	 * @param months Number of months.
	 * @return Retrieved <b>LocalDateTime</b> value.
	 */
	public static LocalDateTime plusMonths(Object source, Integer months) {
		return plusCore(source, months, ChronoUnit.MONTHS);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Retrieves the date time that is a certain number of years before or after the source time.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source Source date time object.
	 * @param years Number of years.
	 * @return Retrieved <b>LocalDateTime</b> value.
	 */
	public static LocalDateTime plusYears(Object source, Integer years) {
		return plusCore(source, years, ChronoUnit.YEARS);
	}

	private static LocalDateTime plusCore(Object source, Integer spanValue, ChronoUnit unit) {
		LocalDateTime result = parse(source);
		if (result == null) {
			return null;
		}
		return result.plus(spanValue != null ? spanValue : 0L, unit);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Retrieves the date time that is the specified duration before or after the source time.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source Source date time object.
	 * @param duration Target duration span.
	 * @return Retrieved <b>LocalDateTime</b> value.
	 */
	public static LocalDateTime plusDuration(Object source, Duration duration) {
		LocalDateTime result = parse(source);
		if (result == null || duration == null) {
			return result;
		}
		return result.plus(duration);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Gets the number of seconds between the two target date time objects.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source1 The first target date time object.
	 * @param source2 The second target date time object.
	 * @return Number of second span.
	 */
	public static long getSecondSpanBetween(Object source1, Object source2) {
		return getSpanBetweenCore(source1, source2, ChronoUnit.SECONDS);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Gets the number of minutes between the two target date time objects.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source1 The first target date time object.
	 * @param source2 The second target date time object.
	 * @return Number of minute span.
	 */
	public static long getMinuteSpanBetween(Object source1, Object source2) {
		return getSpanBetweenCore(source1, source2, ChronoUnit.MINUTES);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Gets the number of hours between the two target date time objects.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source1 The first target date time object.
	 * @param source2 The second target date time object.
	 * @return Number of hour span.
	 */
	public static long getHourSpanBetween(Object source1, Object source2) {
		return getSpanBetweenCore(source1, source2, ChronoUnit.HOURS);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Gets the number of days between the two target date time objects.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source1 The first target date time object.
	 * @param source2 The second target date time object.
	 * @return Number of day span.
	 */
	public static long getDaySpanBetween(Object source1, Object source2) {
		return getSpanBetweenCore(source1, source2, ChronoUnit.DAYS);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Gets the number of weeks between the two target date time objects.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source1 The first target date time object.
	 * @param source2 The second target date time object.
	 * @return Number of week span.
	 */
	public static long getWeekSpanBetween(Object source1, Object source2) {
		return getSpanBetweenCore(source1, source2, ChronoUnit.WEEKS);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Gets the number of months between the two target date time objects.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source1 The first target date time object.
	 * @param source2 The second target date time object.
	 * @return Number of month span.
	 */
	public static long getMonthSpanBetween(Object source1, Object source2) {
		return getSpanBetweenCore(source1, source2, ChronoUnit.MONTHS);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Gets the number of years between the two target date time objects.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source1 The first target date time object.
	 * @param source2 The second target date time object.
	 * @return Number of year span.
	 */
	public static long getYearSpanBetween(Object source1, Object source2) {
		return getSpanBetweenCore(source1, source2, ChronoUnit.YEARS);
	}

	private static long getSpanBetweenCore(Object source1, Object source2, ChronoUnit unit) {
		LocalDateTime datetime1 = parse(source1), datetime2 = parse(source2);
		if (datetime1 == null || datetime2 == null) {
			return 0L;
		}
		return datetime1.until(datetime2, unit);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Gets the duration between the two target date time objects.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source1 The first target date time object.
	 * @param source2 The second target date time object.
	 * @return <b>Duration</b> date time span.
	 */
	public static Duration getDurationBetween(Object source1, Object source2) {
		LocalDateTime dateTime1 = parse(source1), dateTime2 = parse(source2);
		if (dateTime1 == null || dateTime2 == null) {
			return Duration.ZERO;
		}
		return Duration.between(dateTime1, dateTime2);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Fetches the latest date time among the target date time objects.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param sources Target date time objects.
	 * @return The latest <b>LocalTimeTime</b> value.
	 */
	public static LocalDateTime max(Object... sources) {
		LocalDateTime result = null;
		for (Object source : sources) {
			LocalDateTime candidate = parse(source);
			int compareResult = compare(candidate, result);
			if (compareResult == 1 || compareResult == 2) {
				result = candidate;
			}
		}
		return result;
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Fetches the earliest date time among the target date time objects.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param sources Target date time objects.
	 * @return The latest <b>LocalTimeTime</b> value.
	 */
	public static LocalDateTime min(Object... sources) {
		LocalDateTime result = null;
		for (Object source : sources) {
			LocalDateTime candidate = parse(source);
			int compareResult = compare(candidate, result);
			if (compareResult == -1 || compareResult == 2) {
				result = candidate;
			}
		}
		return result;
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Evaluates if the target date time objects are in order from the earliest to the latest.<br>
	 * Adjacent arguments with same value are considered in proper sequence, hence no effect to the evaluating result.<br>
	 * Any argument whose parse result is {@code null} will be ignored, hence no effect to the evaluating result.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;areInSequence("2002-07-21 12:30:00", "2002-07-22 12:30:00") -> true
	 * &#9;areInSequence("2002-07-22 12:30:00", "2002-07-21 12:30:00") -> false
	 * &#9;areInSequence("2002-07-21 12:30:00", "2002-07-21 12:30:00") -> true
	 * &#9;areInSequence("2002-07-21 12:30:00", "2002-07-40 12:30:00") -> true</pre>
	 * @param comparandObjects Target date time objects to be compared.
	 * @return {@code true} if in proper sequence.
	 */
	public static boolean areInSequence(Object... comparandObjects) {
		return areInSequenceCore(comparandObjects, SEQUENCE_INVALID_COMPARE_RESULT_PLAIN);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Evaluates if the target date time objects are in order from the earliest to the latest.<br>
	 * <font color="#EE2222"><b>Appearance of adjacent arguments with same value is considered to be violating the proper sequence.</b></font><br>
	 * Any argument whose parse result is {@code null} will be ignored, hence no effect to the evaluating result.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;areInSequenceNotEqual("2002-07-21 12:30:00", "2002-07-22 12:30:00") -> true
	 * &#9;areInSequenceNotEqual("2002-07-22 12:30:00", "2002-07-21 12:30:00") -> false
	 * &#9;areInSequenceNotEqual("2002-07-21 12:30:00", "2002-07-21 12:30:00") -> false
	 * &#9;areInSequenceNotEqual("2002-07-21 12:30:00", "2002-07-40 12:30:00") -> true</pre>
	 * @param comparandObjects Target date time objects to be compared.
	 * @return {@code true} if in proper sequence.
	 */
	public static boolean areInSequenceNotEqual(Object... comparandObjects) {
		return areInSequenceCore(comparandObjects, SEQUENCE_INVALID_COMPARE_RESULT_NOT_EQUAL);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Evaluates if the target date time objects are in order from the earliest to the latest.<br>
	 * Adjacent arguments with same value are considered in proper sequence, hence no effect to the evaluating result.<br>
	 * <font color="#EE2222"><b>Any argument whose parse result is {@code null} is considered to be violating the proper sequence.</b></font><br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;areInSequenceNotNull("2002-07-21 12:30:00", "2002-07-22 12:30:00") -> true
	 * &#9;areInSequenceNotNull("2002-07-22 12:30:00", "2002-07-21 12:30:00") -> false
	 * &#9;areInSequenceNotNull("2002-07-21 12:30:00", "2002-07-21 12:30:00") -> true
	 * &#9;areInSequenceNotNull("2002-07-21 12:30:00", "2002-07-40 12:30:00") -> false</pre>
	 * @param comparandObjects Target date time objects to be compared.
	 * @return {@code true} if in proper sequence.
	 */
	public static boolean areInSequenceNotNull(Object... comparandObjects) {
		return areInSequenceCore(comparandObjects, SEQUENCE_INVALID_COMPARE_RESULT_NOT_NULL);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Evaluates if the target date time objects are in order from the earliest to the latest.<br>
	 * <font color="#EE2222"><b>Appearance of adjacent arguments with same value is considered to be violating the proper sequence.</b></font><br>
	 * <font color="#EE2222"><b>Any argument whose parse result is {@code null} is considered to be violating the proper sequence.</b></font><br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;areInSequenceNotEqualNull("2002-07-21 12:30:00", "2002-07-22 12:30:00") -> true
	 * &#9;areInSequenceNotEqualNull("2002-07-22 12:30:00", "2002-07-21 12:30:00") -> false
	 * &#9;areInSequenceNotEqualNull("2002-07-21 12:30:00", "2002-07-21 12:30:00") -> false
	 * &#9;areInSequenceNotEqualNull("2002-07-21 12:30:00", "2002-07-40 12:30:00") -> false</pre>
	 * @param comparandObjects Target date time objects to be compared.
	 * @return {@code true} if in proper sequence.
	 */
	public static boolean areInSequenceNotEqualNull(Object... comparandObjects) {
		return areInSequenceCore(comparandObjects, SEQUENCE_INVALID_COMPARE_RESULT_NOT_EQUAL_NULL);
	}

	private static boolean areInSequenceCore(Object[] comparandObjects, List<Integer> sequenceInvalidCompareResult) {
		return areInSequenceCore(comparandObjects, sequenceInvalidCompareResult, DateTimeUtil::compare, DateTimeUtil::isDateTime);
	}

	private static boolean areInSequenceCore(Object[] comparandObjects, List<Integer> sequenceInvalidCompareResult,
			BiFunction<Object, Object, Integer> compareMethod, Function<Object, Boolean> validateMethod) {
		Object previousValidComparand = comparandObjects.length != 0 ? comparandObjects[0] : null;
		for (int index = 1; index < comparandObjects.length; index ++) {
			Object thisComparand = comparandObjects[index];
			if (sequenceInvalidCompareResult.contains(compareMethod.apply(previousValidComparand, thisComparand))) {
				return false;
			}
			previousValidComparand = validateMethod.apply(thisComparand) ? thisComparand : previousValidComparand;
		}
		return true;
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Evaluates size relationship between the two target date time objects.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param comparandObject1 The first target datetime object to be compared.
	 * @param comparandObject2 The second target datetime object to be compared.
	 * @return Comparison result.<br>
	 * The following comparison results are for reference.<br><br>
	 * <table border style="width: 240px; text-align: center;">
	 * <tr><td><b>0</b></td><td>comparand1 = comparand2</td></tr>
	 * <tr><td><b>1</b></td><td>comparand1 > comparand2</td></tr>
	 * <tr><td><b>-1</b></td><td>comparand1 < comparand2</td></tr>
	 * <tr><td><b>2</b></td><td>only comparand2 is {@code null}</td></tr>
	 * <tr><td><b>-2</b></td><td>only comparand1 is {@code null}</td></tr>
	 * <tr><td><b>22</b></td><td>both comparands are {@code null}</td></tr>
	 * </table>
	 */
	public static int compare(Object comparandObject1, Object comparandObject2) {
		LocalDateTime dateTime1 = parse(comparandObject1), dateTime2 = parse(comparandObject2);
		if (dateTime1 == null && dateTime2 == null) {
			return 22;
		}
		if (dateTime1 == null) {
			return -2;
		}
		if (dateTime2 == null) {
			return 2;
		}
		if (dateTime1.isEqual(dateTime2)) {
			return 0;
		}
		if (dateTime1.isAfter(dateTime2)) {
			return 1;
		}
		if (dateTime1.isBefore(dateTime2)) {
			return -1;
		}
		return 0;
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Provides an ascending <b>Comparator&lt;Type&gt;</b> available in methods such as {@link List#sort(Comparator)}.<br>
	 * Uses {@link #compareTime(Object, Object)} as base method.
	 * <pre><b><i>Eg.:</i></b>&#9;eventList.sort(DecimalUtil.compareAsc(Event::getEventDateTime))</pre>
	 * @param <Type> The type of object to be compared.
	 * @param fieldGetter The getter of the field to be used for comparing.
	 * @return <b>Comparator&lt;Type&gt;</b><br>
	 */
	public static <Type> Comparator<Type> compareAsc(Function<Type, Object> fieldGetter) {
		return (entity1, entity2) -> compare(fieldGetter.apply(entity1), fieldGetter.apply(entity2));
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Provides a descending <b>Comparator&lt;Type&gt;</b> available in methods such as {@link List#sort(Comparator)}.<br>
	 * Uses {@link #compare(Object, Object)} as base method.
	 * <pre><b><i>Eg.:</i></b>&#9;eventList.sort(DecimalUtil.compareDesc(Event::getEventDateTime))</pre>
	 * @param <Type> The type of object to be compared.
	 * @param fieldGetter The getter of the field to be used for comparing.
	 * @return <b>Comparator&lt;Type&gt;</b><br>
	 */
	public static <Type> Comparator<Type> compareDesc(Function<Type, Object> fieldGetter) {
		return (entity1, entity2) -> compare(fieldGetter.apply(entity2), fieldGetter.apply(entity1));
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// datetime output

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Efficiently formats the current date time with the preset format: <b><u>yyyyMMddHHmm</u></b>,<br>
	 * eg.: <b><u>200207211230</u></b>.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String now_shortPlain() {
		return now(DATE_TIME_SHORT_PLAIN);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Efficiently formats the current date time with the preset format: <b><u>yyyy/MM/dd HH:mm</u></b>,<br>
	 * eg.: <b><u>2002/07/21 12:30</u></b>.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String now_shortSlashColon() {
		return now(DATE_TIME_SHORT_SLASH_COLON);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Efficiently formats the current date time with the preset format: <b><u>yyyy-MM-dd HH:mm</u></b>,<br>
	 * eg.: <b><u>2002-07-21 12:30</u></b>.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String now_shortDashColon() {
		return now(DATE_TIME_SHORT_DASH_COLON);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Efficiently formats the current date time with the preset format: <b><u>yyyyMMddHHmmss</u></b>,<br>
	 * eg.: <b><u>20020721123050</u></b>.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String now_basicPlain() {
		return now(DATE_TIME_BASIC_PLAIN);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Efficiently formats the current date time with the preset format: <b><u>yyyy/MM/dd HH:mm:ss</u></b>,<br>
	 * eg.: <b><u>2002/07/21 12:30:50</u></b>.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String now_basicSlashColon() {
		return now(DATE_TIME_BASIC_SLASH_COLON);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Efficiently formats the current date time with the preset format: <b><u>yyyy-MM-dd HH:mm:ss</u></b>,<br>
	 * eg.: <b><u>2002-07-21 12:30:50</u></b>.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String now_basicDashColon() {
		return now(DATE_TIME_BASIC_DASH_COLON);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Efficiently formats the current date time with the preset format: <b><u>yyyyMMddHHmmssSSS</u></b>,<br>
	 * eg.: <b><u>20020721123050000</u></b>.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String now_fullPlain() {
		return now(DATE_TIME_FULL_PLAIN);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Efficiently formats the current date time with the preset format: <b><u>yyyy/MM/dd HH:mm:ss.SSS</u></b>,<br>
	 * eg.: <b><u>2002/07/21 12:30:50.000</u></b>.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String now_fullSlashColon() {
		return now(DATE_TIME_FULL_SLASH_COLON);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Efficiently formats the current date time with the preset format: <b><u>yyyy-MM-dd HH:mm:ss.SSS</u></b>,<br>
	 * eg.: <b><u>2002-07-21 12:30:50.000</u></b>.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String now_fullDashColon() {
		return now(DATE_TIME_FULL_DASH_COLON);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Efficiently formats the current date time with the specified format.
	 * @param formatPattern Target date time format presented by a <b>String</b> char sequence.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String now(String formatPattern) {
		return now(DateTimeFormatter.ofPattern(formatPattern));
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Efficiently formats the current date time with the specified format.
	 * @param format Target date time format presented by a {@link DateTimeFormatter} object.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String now(DateTimeFormatter format) {
		return format(LocalTime.now(), format);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Efficiently formats the target date time with the preset format: <b><u>yyyyMMddHHmm</u></b>,<br>
	 * eg.: <b><u>200207211230</u></b>.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source Target object to be parsed and formatted.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String format_shortPlain(Object source) {
		return format(source, DATE_TIME_SHORT_PLAIN);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Efficiently formats the target date time with the preset format: <b><u>yyyy/MM/dd HH:mm</u></b>,<br>
	 * eg.: <b><u>2002/07/21 12:30</u></b>.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source Target object to be parsed and formatted.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String format_shortSlashColon(Object source) {
		return format(source, DATE_TIME_SHORT_SLASH_COLON);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Efficiently formats the target date time with the preset format: <b><u>yyyy-MM-dd HH:mm</u></b>,<br>
	 * eg.: <b><u>2002-07-21 12:30</u></b>.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source Target object to be parsed and formatted.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String format_shortDashColon(Object source) {
		return format(source, DATE_TIME_SHORT_DASH_COLON);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Efficiently formats the target date time with the preset format: <b><u>yyyyMMddHHmmss</u></b>,<br>
	 * eg.: <b><u>20020721123050</u></b>.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source Target object to be parsed and formatted.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String format_basicPlain(Object source) {
		return format(source, DATE_TIME_BASIC_PLAIN);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Efficiently formats the target date time with the preset format: <b><u>yyyy/MM/dd HH:mm:ss</u></b>,<br>
	 * eg.: <b><u>2002/07/21 12:30:50</u></b>.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source Target object to be parsed and formatted.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String format_basicSlashColon(Object source) {
		return format(source, DATE_TIME_BASIC_SLASH_COLON);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Efficiently formats the target date time with the preset format: <b><u>yyyy-MM-dd HH:mm:ss</u></b>,<br>
	 * eg.: <b><u>2002-07-21 12:30:50</u></b>.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source Target object to be parsed and formatted.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String format_basicDashColon(Object source) {
		return format(source, DATE_TIME_BASIC_DASH_COLON);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Efficiently formats the target date time with the preset format: <b><u>yyyyMMddHHmmssSSS</u></b>,<br>
	 * eg.: <b><u>20020721123050000</u></b>.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source Target object to be parsed and formatted.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String format_fullPlain(Object source) {
		return format(source, DATE_TIME_FULL_PLAIN);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Efficiently formats the target date time with the preset format: <b><u>yyyy/MM/dd HH:mm:ss.SSS</u></b>,<br>
	 * eg.: <b><u>2002/07/21 12:30:50.000</u></b>.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source Target object to be parsed and formatted.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String format_fullSlashColon(Object source) {
		return format(source, DATE_TIME_FULL_SLASH_COLON);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Efficiently formats the target date time with the preset format: <b><u>yyyy-MM-dd HH:mm:ss.SSS</u></b>,<br>
	 * eg.: <b><u>2002-07-21 12:30:50.000</u></b>.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source Target object to be parsed and formatted.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String format_fullDashColon(Object source) {
		return format(source, DATE_TIME_FULL_DASH_COLON);
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Efficiently formats the target date time in specified format.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source Target object to be parsed and formatted.
	 * @param formatPattern Target date time format presented by a <b>String</b> char sequence.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String format(Object source, String formatPattern) {
		return format(source, DateTimeFormatter.ofPattern(formatPattern));
	}

	/**
	 * <font color="EE22EE"><b>DateTime operation.</b></font><br>
	 * Efficiently formats the target date time in specified format.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source Target object to be parsed and formatted.
	 * @param format Target date time format presented by a {@link DateTimeFormatter} object.
	 * @return Formatted <b>String</b> char sequence.
	 */
	public static String format(Object source, DateTimeFormatter format) {
		LocalDateTime result = parse(source);
		if (result == null) {
			return null;
		}
		return result.format(format);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// other output

	/**
	 * Parses and wraps the target object into a <b>Date</b> instance.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source Target object to be parsed and wrapped.
	 * @return Wrapped <b>Date</b> value.
	 */
	public static Date toDate(Object source) {
		LocalDateTime dateTime = parse(source);
		if (dateTime == null) {
			return null;
		}
		return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
	}

	/**
	 * Parses and wraps the target object into a <b>Timestamp</b> instance.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source Target object to be parsed and wrapped.
	 * @return Wrapped <b>Timestamp</b> value.
	 */
	public static Timestamp toTimestamp(Object source) {
		LocalDateTime dateTime = parse(source);
		if (dateTime == null) {
			return null;
		}
		return Timestamp.valueOf(dateTime);
	}

	/**
	 * Parses and wraps the target object into a <b>Calendar</b> instance.<br>
	 * Uses {@link #parse(Object)} for automatic parsing.
	 * @param source Target object to be parsed and wrapped.
	 * @return Wrapped <b>Calendar</b> value.
	 */
	public static Calendar toCalendar(Object source) {
		Date date = toDate(source);
		if (date == null) {
			return null;
		}
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// constants
	// output methods for formats with _ in name is not offered

	/** <font color="#2222EE"><b>Date format</b></font> of pattern: <b><u>yyyyMMdd</u></b>, eg.: <b><u>20020721</u></b>. */
	public static final DateTimeFormatter DATE_FULL_PLAIN = DateTimeFormatter.ofPattern("yyyyMMdd");
	/** <font color="#2222EE"><b>Date format</b></font> of pattern: <b><u>yyyy/MM/dd</u></b>, eg.: <b><u>2002/07/21</u></b>. */
	public static final DateTimeFormatter DATE_FULL_SLASH = DateTimeFormatter.ofPattern("yyyy/MM/dd");
	/** <font color="#2222EE"><b>Date format</b></font> of pattern: <b><u>yyyy-MM-dd</u></b>, eg.: <b><u>2002-07-21</u></b>. */
	public static final DateTimeFormatter DATE_FULL_DASH = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	/** <font color="#2222EE"><b>Date format</b></font> of pattern: <b><u>yyyy年MM月dd日</u></b>, eg.: <b><u>2002年07月21日</u></b>. */
	public static final DateTimeFormatter DATE_FULL_CHAR = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
	/** <font color="#2222EE"><b>Date format</b></font> of pattern: <b><u>yyyy/M/d</u></b>, eg.: <b><u>2002/7/21</u></b>. */
	public static final DateTimeFormatter _DATE_SHORT_MD_SLASH = DateTimeFormatter.ofPattern("yyyy/M/d");
	/** <font color="#2222EE"><b>Date format</b></font> of pattern: <b><u>yyyy-M-d</u></b>, eg.: <b><u>2002-7-21</u></b>. */
	public static final DateTimeFormatter _DATE_SHORT_MD_DASH = DateTimeFormatter.ofPattern("yyyy-M-d");
	/** <font color="#2222EE"><b>Date format</b></font> of pattern: <b><u>yyyy年M月d日</u></b>, eg.: <b><u>2002年7月21日</u></b>. */
	public static final DateTimeFormatter _DATE_SHORT_MD_CHAR = DateTimeFormatter.ofPattern("yyyy年M月d日");
	/** <font color="#2222EE"><b>Date format</b></font> of pattern: <b><u>yyMMdd</u></b>, eg.: <b><u>020721</u></b>. */
	public static final DateTimeFormatter _DATE_SHORT_Y_PLAIN = DateTimeFormatter.ofPattern("yyMMdd");
	/** <font color="#2222EE"><b>Date format</b></font> of pattern: <b><u>yy/MM/dd</u></b>, eg.: <b><u>02/07/21</u></b>. */
	public static final DateTimeFormatter _DATE_SHORT_Y_SLASH = DateTimeFormatter.ofPattern("yy/MM/dd");
	/** <font color="#2222EE"><b>Date format</b></font> of pattern: <b><u>yy-MM-dd</u></b>, eg.: <b><u>02-07-21</u></b>. */
	public static final DateTimeFormatter _DATE_SHORT_Y_DASH = DateTimeFormatter.ofPattern("yy-MM-dd");
	/** <font color="#2222EE"><b>Date format</b></font> of pattern: <b><u>MMddyy</u></b>, eg.: <b><u>072102</u></b>. */
	public static final DateTimeFormatter _DATE_MDY_PLAIN = DateTimeFormatter.ofPattern("MMddyy");

	/** <font color="#EE2222"><b>Time format</b></font> of pattern: <b><u>HHmm</u></b>, eg.: <b><u>1230</u></b>. */
	public static final DateTimeFormatter TIME_SHORT_PLAIN = DateTimeFormatter.ofPattern("HHmm");
	/** <font color="#EE2222"><b>Time format</b></font> of pattern: <b><u>HH:mm</u></b>, eg.: <b><u>12:30</u></b>. */
	public static final DateTimeFormatter TIME_SHORT_COLON = DateTimeFormatter.ofPattern("HH:mm");
	/** <font color="#EE2222"><b>Time format</b></font> of pattern: <b><u>HHmmss</u></b>, eg.: <b><u>123050</u></b>. */
	public static final DateTimeFormatter TIME_BASIC_PLAIN = DateTimeFormatter.ofPattern("HHmmss");
	/** <font color="#EE2222"><b>Time format</b></font> of pattern: <b><u>HH:mm:ss</u></b>, eg.: <b><u>12:30:50</u></b>. */
	public static final DateTimeFormatter TIME_BASIC_COLON = DateTimeFormatter.ofPattern("HH:mm:ss");
	/** <font color="#EE2222"><b>Time format</b></font> of pattern: <b><u>HHmmssSSS</u></b>, eg.: <b><u>123050000</u></b>. */
	public static final DateTimeFormatter TIME_FULL_PLAIN = DateTimeFormatter.ofPattern("HHmmssSSS");
	/** <font color="#EE2222"><b>Time format</b></font> of pattern: <b><u>HH:mm:ss.SSS</u></b>, eg.: <b><u>12:30:05.000</u></b>. */
	public static final DateTimeFormatter TIME_FULL_COLON = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

	/** <font color="EE22EE"><b>DateTime format</b></font> of pattern: <b><u>yyyyMMddHHmm</u></b>, eg.: <b><u>200207211230</u></b>. */
	public static final DateTimeFormatter DATE_TIME_SHORT_PLAIN = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
	/** <font color="EE22EE"><b>DateTime format</b></font> of pattern: <b><u>yyyy/MM/dd HH:mm</u></b>, eg.: <b><u>2002/07/21 12:30</u></b>. */
	public static final DateTimeFormatter DATE_TIME_SHORT_SLASH_COLON = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
	/** <font color="EE22EE"><b>DateTime format</b></font> of pattern: <b><u>yyyy-MM-dd HH:mm</u></b>, eg.: <b><u>2002-07-21 12:30</u></b>. */
	public static final DateTimeFormatter DATE_TIME_SHORT_DASH_COLON = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	/** <font color="EE22EE"><b>DateTime format</b></font> of pattern: <b><u>yyyyMMddHHmmss</u></b>, eg.: <b><u>20020721123050</u></b>. */
	public static final DateTimeFormatter DATE_TIME_BASIC_PLAIN = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
	/** <font color="EE22EE"><b>DateTime format</b></font> of pattern: <b><u>yyyy/MM/dd HH:mm:ss</u></b>, eg.: <b><u>2002/07/21 12:30:50</u></b>. */
	public static final DateTimeFormatter DATE_TIME_BASIC_SLASH_COLON = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
	/** <font color="EE22EE"><b>DateTime format</b></font> of pattern: <b><u>yyyy-MM-dd HH:mm:ss</u></b>, eg.: <b><u>2002-07-21 12:30:50</u></b>. */
	public static final DateTimeFormatter DATE_TIME_BASIC_DASH_COLON = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	/** <font color="EE22EE"><b>DateTime format</b></font> of pattern: <b><u>yyyyMMddHHmmssSSS</u></b>, eg.: <b><u>20020721123050000</u></b>. */
	public static final DateTimeFormatter DATE_TIME_FULL_PLAIN = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
	/** <font color="EE22EE"><b>DateTime format</b></font> of pattern: <b><u>yyyy/MM/dd HH:mm:ss.SSS</u></b>, eg.: <b><u>2002/07/21 12:30:50.000</u></b>. */
	public static final DateTimeFormatter DATE_TIME_FULL_SLASH_COLON = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS");
	/** <font color="EE22EE"><b>DateTime format</b></font> of pattern: <b><u>yyyy-MM-dd HH:mm:ss.SSS</u></b>, eg.: <b><u>2002-07-21 12:30:50.000</u></b>. */
	public static final DateTimeFormatter DATE_TIME_FULL_DASH_COLON = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

	private static final String YEAR_COMPLEMENT = "1231";
	private static final String HOUR_COMPLEMENT = "0000";
	private static final Map<DateTimeFormatter, String> PRESET_DATE_FORMAT;
	private static final Map<DateTimeFormatter, String> PRESET_TIME_FORMAT;
	private static final Map<DateTimeFormatter, String> PRESET_DATE_TIME_FORMAT;

	/**
	 * Historical chronology of Japan.<br><br>
	 * <table border="1px solid #000000" style="width: 180px; text-align: center;">
	 * <th><td><b>begin</b></td><td><b>end</td></b></th>
	 * <tr><td><b>明治</b></td><td>1868</td><td>1911</td></tr>
	 * <tr><td><b>大正</b></td><td>1912</td><td>1925</td></tr>
	 * <tr><td><b>昭和</b></td><td>1926</td><td>1988</td></tr>
	 * <tr><td><b>平成</b></td><td>1989</td><td>2018</td></tr>
	 * <tr><td><b>令和</b></td><td>2019</td><td>-</td></tr>
	 * </table>
	 */
	public static final Map<String, EraYearSpan> JP_ERA_NAME;
	/**
	 * Day of the week with the Japanese format.<br><br>
	 * <table border="1px solid #000000" style="width: 120px; text-align: center;">
	 * <th><td><b>week day</b></td></th>
	 * <tr><td><b>日</b></td><td>0/7</td></tr>
	 * <tr><td><b>月</b></td><td>1</td></tr>
	 * <tr><td><b>火</b></td><td>2</td></tr>
	 * <tr><td><b>水</b></td><td>3</td></tr>
	 * <tr><td><b>木</b></td><td>4</td></tr>
	 * <tr><td><b>金</b></td><td>5</td></tr>
	 * <tr><td><b>土</b></td><td>6</td></tr>
	 * </table>
	 */
	public static final Map<String, List<Object>> JP_DAY_OF_WEEK_NAME;
	private static final List<Integer> SEQUENCE_INVALID_COMPARE_RESULT_PLAIN = Collections.unmodifiableList(Arrays.asList(1));
	private static final List<Integer> SEQUENCE_INVALID_COMPARE_RESULT_NOT_EQUAL = Collections.unmodifiableList(Arrays.asList(1, 0));
	private static final List<Integer> SEQUENCE_INVALID_COMPARE_RESULT_NOT_NULL = Collections.unmodifiableList(Arrays.asList(1, 2, -2, 22));
	private static final List<Integer> SEQUENCE_INVALID_COMPARE_RESULT_NOT_EQUAL_NULL = Collections.unmodifiableList(Arrays.asList(1, 0, 2, -2, 22));
	private static final String EMPTY = EmbeddedStringUtil.EMPTY;

	static {
		Map<DateTimeFormatter, String> presetDateFormatMap = new HashMap<>();
		Map<DateTimeFormatter, String> presetTimeFormatMap = new HashMap<>();
		Map<DateTimeFormatter, String> presetDateTimeFormatMap = new HashMap<>();

		presetDateFormatMap.put(DATE_FULL_PLAIN, "01");
		presetDateFormatMap.put(DATE_FULL_SLASH, "/01");
		presetDateFormatMap.put(DATE_FULL_DASH, "-01");
		presetDateFormatMap.put(DATE_FULL_CHAR, "01日");
		presetDateFormatMap.put(_DATE_SHORT_MD_SLASH, "/1");
		presetDateFormatMap.put(_DATE_SHORT_MD_DASH, "-1");
		presetDateFormatMap.put(_DATE_SHORT_MD_CHAR, "1日");
		presetDateFormatMap.put(_DATE_SHORT_Y_PLAIN, "01");
		presetDateFormatMap.put(_DATE_SHORT_Y_SLASH, "/01");
		presetDateFormatMap.put(_DATE_SHORT_Y_DASH, "-01");
		presetDateFormatMap.put(_DATE_MDY_PLAIN, EMPTY);

		presetTimeFormatMap.put(TIME_SHORT_PLAIN, EMPTY);
		presetTimeFormatMap.put(TIME_SHORT_COLON, EMPTY);
		presetTimeFormatMap.put(TIME_BASIC_PLAIN, "00");
		presetTimeFormatMap.put(TIME_BASIC_COLON, ":00");
		presetTimeFormatMap.put(TIME_FULL_PLAIN, "00000");
		presetTimeFormatMap.put(TIME_FULL_COLON, ":00.000");

		presetDateTimeFormatMap.put(DATE_TIME_SHORT_PLAIN, EMPTY);
		presetDateTimeFormatMap.put(DATE_TIME_SHORT_SLASH_COLON, EMPTY);
		presetDateTimeFormatMap.put(DATE_TIME_SHORT_DASH_COLON, EMPTY);
		presetDateTimeFormatMap.put(DATE_TIME_BASIC_PLAIN, EMPTY);
		presetDateTimeFormatMap.put(DATE_TIME_BASIC_SLASH_COLON, EMPTY);
		presetDateTimeFormatMap.put(DATE_TIME_BASIC_DASH_COLON, EMPTY);
		presetDateTimeFormatMap.put(DATE_TIME_FULL_PLAIN, EMPTY);
		presetDateTimeFormatMap.put(DATE_TIME_FULL_SLASH_COLON, EMPTY);
		presetDateTimeFormatMap.put(DATE_TIME_FULL_DASH_COLON, EMPTY);

		PRESET_DATE_FORMAT = Collections.unmodifiableMap(presetDateFormatMap);
		PRESET_TIME_FORMAT = Collections.unmodifiableMap(presetTimeFormatMap);
		PRESET_DATE_TIME_FORMAT = Collections.unmodifiableMap(presetDateTimeFormatMap);

		Map<String, EraYearSpan> jpEraNameMap = new HashMap<>();
		Map<String, List<Object>> jpDayOfWeekNameMap = new HashMap<>();

		jpEraNameMap.put("明治", new EraYearSpan(1868, 1911));
		jpEraNameMap.put("大正", new EraYearSpan(1912, 1925));
		jpEraNameMap.put("昭和", new EraYearSpan(1926, 1988));
		jpEraNameMap.put("平成", new EraYearSpan(1989, 2018));
		jpEraNameMap.put("令和", new EraYearSpan(2019, 9999));

		jpDayOfWeekNameMap.put("日", Arrays.asList(0, "0", 7, "7", DayOfWeek.SUNDAY));
		jpDayOfWeekNameMap.put("月", Arrays.asList(1, "1", DayOfWeek.MONDAY));
		jpDayOfWeekNameMap.put("火", Arrays.asList(2, "2", DayOfWeek.TUESDAY));
		jpDayOfWeekNameMap.put("水", Arrays.asList(3, "3", DayOfWeek.WEDNESDAY));
		jpDayOfWeekNameMap.put("木", Arrays.asList(4, "4", DayOfWeek.THURSDAY));
		jpDayOfWeekNameMap.put("金", Arrays.asList(5, "5", DayOfWeek.FRIDAY));
		jpDayOfWeekNameMap.put("土", Arrays.asList(6, "6", DayOfWeek.SATURDAY));

		JP_ERA_NAME = Collections.unmodifiableMap(jpEraNameMap);
		JP_DAY_OF_WEEK_NAME = Collections.unmodifiableMap(jpDayOfWeekNameMap);
	}

	private static class EraYearSpan {
		private final int begin;
		private final int end;
		EraYearSpan(int begin, int end) {
			this.begin = begin;
			this.end = end;
		}
		public int getBegin() {
			return begin;
		}
		public int getEnd() {
			return end;
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// embedded utils

	private static class EmbeddedStringUtil {
		private static final String EMPTY = "";
		private static final List<Character> SPACE_CHAR = Collections.unmodifiableList(Arrays.asList(' ', '　', '	'));
		private static String FHTrim(String source) {
			return FHLTrim(FHRTrim(source));
		}
		private static String FHLTrim(String source) {
			if (source == null || source.equals(EMPTY)) {
				return source;
			}
			int pos = 0;
			for (int index = 0; index < source.length(); index ++) {
				char c = source.charAt(index);
				if (!SPACE_CHAR.contains(c)) {
					break;
				}
				pos = index + 1;
			}
			if (pos > 0) {
				return source.substring(pos);
			}
			return source;
		}
		private static String FHRTrim(String source) {
			if (source == null || source.equals(EMPTY)) {
				return source;
			}
			int pos = 0;
			for (int index = source.length() - 1; index >= 0; index --) {
				char c = source.charAt(index);
				if (!SPACE_CHAR.contains(c)) {
					break;
				}
				pos = index;
			}
			if (pos > 0) {
				return source.substring(0, pos);
			}
			return source;
		}

		static boolean isNullOrBlank(String source) {
			return source == null || FHTrim(source).length() == 0;
		}
	}

}
