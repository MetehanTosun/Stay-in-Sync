package predicates.datetime_predicates;

import de.unistuttgart.graphengine.logic_operator.datetime_predicates.DateTimeParserUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the DateTimeParserUtil.
 */
@DisplayName("DateTimeParserUtil")
public class DateTimeParserUtilTest {

    @Test
    @DisplayName("should return null when input is null")
    void testToZonedDateTime_WithNull_ShouldReturnNull() {
        // ACT
        ZonedDateTime result = DateTimeParserUtil.toZonedDateTime(null);

        // ASSERT
        assertNull(result);
    }

    @Test
    @DisplayName("should parse full ISO 8601 string with UTC timezone")
    void testToZonedDateTime_WithFullIsoStringUTC_ShouldParseCorrectly() {
        // ARRANGE
        String input = "2025-10-02T14:30:00Z";

        // ACT
        ZonedDateTime result = DateTimeParserUtil.toZonedDateTime(input);

        // ASSERT
        assertNotNull(result);
        assertEquals(2025, result.getYear());
        assertEquals(10, result.getMonthValue());
        assertEquals(2, result.getDayOfMonth());
        assertEquals(14, result.getHour());
        assertEquals(30, result.getMinute());
        assertEquals(0, result.getSecond());
        assertEquals(ZoneOffset.UTC, result.getOffset());
    }

    @Test
    @DisplayName("should parse full ISO 8601 string with timezone offset")
    void testToZonedDateTime_WithFullIsoStringOffset_ShouldParseCorrectly() {
        // ARRANGE
        String input = "2025-10-02T14:30:00+02:00";

        // ACT
        ZonedDateTime result = DateTimeParserUtil.toZonedDateTime(input);

        // ASSERT
        assertNotNull(result);
        assertEquals(2025, result.getYear());
        assertEquals(10, result.getMonthValue());
        assertEquals(2, result.getDayOfMonth());
        assertEquals(14, result.getHour());
        assertEquals(30, result.getMinute());
        assertEquals(ZoneOffset.ofHours(2), result.getOffset());
    }

    @Test
    @DisplayName("should parse full ISO 8601 string with milliseconds")
    void testToZonedDateTime_WithMilliseconds_ShouldParseCorrectly() {
        // ARRANGE
        String input = "2025-10-02T14:30:00.123Z";

        // ACT
        ZonedDateTime result = DateTimeParserUtil.toZonedDateTime(input);

        // ASSERT
        assertNotNull(result);
        assertEquals(2025, result.getYear());
        assertEquals(10, result.getMonthValue());
        assertEquals(2, result.getDayOfMonth());
        assertEquals(14, result.getHour());
        assertEquals(30, result.getMinute());
        assertEquals(123_000_000, result.getNano());
    }

    @Test
    @DisplayName("should convert date-only string to midnight UTC")
    void testToZonedDateTime_WithDateOnly_ShouldConvertToMidnightUTC() {
        // ARRANGE
        String input = "2025-10-02";

        // ACT
        ZonedDateTime result = DateTimeParserUtil.toZonedDateTime(input);

        // ASSERT
        assertNotNull(result);
        assertEquals(2025, result.getYear());
        assertEquals(10, result.getMonthValue());
        assertEquals(2, result.getDayOfMonth());
        assertEquals(0, result.getHour());
        assertEquals(0, result.getMinute());
        assertEquals(0, result.getSecond());
        assertEquals(ZoneOffset.UTC, result.getOffset());
    }

    @Test
    @DisplayName("should convert date-only string with leap year to midnight UTC")
    void testToZonedDateTime_WithLeapYear_ShouldConvertCorrectly() {
        // ARRANGE
        String input = "2024-02-29";

        // ACT
        ZonedDateTime result = DateTimeParserUtil.toZonedDateTime(input);

        // ASSERT
        assertNotNull(result);
        assertEquals(2024, result.getYear());
        assertEquals(2, result.getMonthValue());
        assertEquals(29, result.getDayOfMonth());
        assertEquals(0, result.getHour());
        assertEquals(ZoneOffset.UTC, result.getOffset());
    }

    @Test
    @DisplayName("should return null for invalid date strings")
    void testToZonedDateTime_WithInvalidString_ShouldReturnNull() {
        // ARRANGE & ACT & ASSERT
        assertNull(DateTimeParserUtil.toZonedDateTime("not-a-date"));
        assertNull(DateTimeParserUtil.toZonedDateTime("2025-13-01")); // Invalid month
        assertNull(DateTimeParserUtil.toZonedDateTime("2025-02-30")); // Invalid day
        assertNull(DateTimeParserUtil.toZonedDateTime("2025-10-02T25:00:00Z")); // Invalid hour
        assertNull(DateTimeParserUtil.toZonedDateTime("2025/10/02")); // Wrong separator
        assertNull(DateTimeParserUtil.toZonedDateTime("02-10-2025")); // Wrong order
        assertNull(DateTimeParserUtil.toZonedDateTime("2025-10-02T14:30")); // Missing seconds
        assertNull(DateTimeParserUtil.toZonedDateTime("")); // Empty string
    }

    @Test
    @DisplayName("should return null for non-string, non-temporal types")
    void testToZonedDateTime_WithInvalidTypes_ShouldReturnNull() {
        // ACT & ASSERT
        assertNull(DateTimeParserUtil.toZonedDateTime(12345));
        assertNull(DateTimeParserUtil.toZonedDateTime(true));
        assertNull(DateTimeParserUtil.toZonedDateTime(new Object()));
    }

    @Test
    @DisplayName("should convert ZonedDateTime input directly")
    void testToZonedDateTime_WithZonedDateTime_ShouldReturnDirectly() {
        // ARRANGE
        ZonedDateTime input = ZonedDateTime.parse("2025-10-02T14:30:00Z");

        // ACT
        ZonedDateTime result = DateTimeParserUtil.toZonedDateTime(input);

        // ASSERT
        assertEquals(input, result);
    }

    @Test
    @DisplayName("should convert LocalDate to ZonedDateTime at midnight UTC")
    void testToZonedDateTime_WithLocalDate_ShouldConvertToZonedDateTime() {
        // ARRANGE
        LocalDate localDate = LocalDate.of(2025, 10, 2);

        // ACT
        ZonedDateTime result = DateTimeParserUtil.toZonedDateTime(localDate);

        // ASSERT
        assertNotNull(result);
        assertEquals(2025, result.getYear());
        assertEquals(10, result.getMonthValue());
        assertEquals(2, result.getDayOfMonth());
        assertEquals(0, result.getHour());
        assertEquals(ZoneOffset.UTC, result.getOffset());
    }

    @Test
    @DisplayName("should preserve different timezones when parsing")
    void testToZonedDateTime_WithDifferentTimezones_ShouldPreserveTimezone() {
        // ARRANGE
        String inputUTC = "2025-10-02T14:30:00Z";
        String inputCEST = "2025-10-02T14:30:00+02:00";
        String inputPST = "2025-10-02T14:30:00-08:00";

        // ACT
        ZonedDateTime resultUTC = DateTimeParserUtil.toZonedDateTime(inputUTC);
        ZonedDateTime resultCEST = DateTimeParserUtil.toZonedDateTime(inputCEST);
        ZonedDateTime resultPST = DateTimeParserUtil.toZonedDateTime(inputPST);

        // ASSERT
        assertEquals(ZoneOffset.UTC, resultUTC.getOffset());
        assertEquals(ZoneOffset.ofHours(2), resultCEST.getOffset());
        assertEquals(ZoneOffset.ofHours(-8), resultPST.getOffset());
    }

    @Test
    @DisplayName("should parse edge case minimum date correctly")
    void testToZonedDateTime_WithMinimumDate_ShouldParseCorrectly() {
        // ARRANGE
        String input = "0001-01-01T00:00:00Z";

        // ACT
        ZonedDateTime result = DateTimeParserUtil.toZonedDateTime(input);

        // ASSERT
        assertNotNull(result);
        assertEquals(1, result.getYear());
        assertEquals(1, result.getMonthValue());
        assertEquals(1, result.getDayOfMonth());
    }

    @Test
    @DisplayName("should parse edge case maximum date correctly")
    void testToZonedDateTime_WithMaximumDate_ShouldParseCorrectly() {
        // ARRANGE
        String input = "9999-12-31T23:59:59Z";

        // ACT
        ZonedDateTime result = DateTimeParserUtil.toZonedDateTime(input);

        // ASSERT
        assertNotNull(result);
        assertEquals(9999, result.getYear());
        assertEquals(12, result.getMonthValue());
        assertEquals(31, result.getDayOfMonth());
    }
}