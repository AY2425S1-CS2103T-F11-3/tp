package seedu.address.model.person;

import static java.util.Objects.isNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import seedu.address.model.person.exceptions.TimeParseException;

/**
 * Represents a Person's Appointment in the address book.
 * Guarantees: immutable; is valid as declared in {@link #isValidAppointment(String)}
 */
public class Appointment {

    public static final String MESSAGE_CONSTRAINTS = "Dates should be in a format of \n"
            + "dd/MM/yyyy HH:mm or dd MM yyyy HH:mm or dd-MM-yyyy HH:mm"
            + "and must be between 8:30 - 21:30 inclusive";
    public static final DateTimeFormatter ENGLISH_FORMAT = DateTimeFormatter.ofPattern("dd MMMM yyyy",
                                                                                            Locale.ENGLISH);
    public static final DateTimeFormatter ENGLISH_FORMAT_WITH_TIME = DateTimeFormatter.ofPattern(
                                                                "dd MMMM yyyy HH:mm",
                                                                        Locale.ENGLISH);
    public static final DateTimeFormatter[] FORMATTERS = new DateTimeFormatter[] {
        ENGLISH_FORMAT_WITH_TIME,
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"),
        DateTimeFormatter.ofPattern("dd MM yyyy HH:mm")
    };
    public static final LocalDate TODAY = LocalDate.now();

    public final String dateTime;

    /**
     * Constructs a {@code Appointment}.
     *
     * @param date A valid Appointment date.
     */
    public Appointment(String date) {
        if (isNull(date) || date.equals("-")) {
            dateTime = "-";
        } else {
            checkArgument(isValidAppointment(date), MESSAGE_CONSTRAINTS);
            dateTime = parse(date).format(ENGLISH_FORMAT_WITH_TIME);
        }
    }

    /**
     * Parses string into LocalDateTime Object based on fixed formats
     * @param dateTime A valid Appointment date
     * @return LocalDateTime object corresponding to a specific date and time
     * @throws TimeParseException String given does not match any time format
     */
    public static LocalDateTime parse(String dateTime) throws TimeParseException {
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDateTime.parse(dateTime, formatter);
            } catch (DateTimeParseException ignored) {
                continue;
            }
        }
        throw new TimeParseException();
    }

    /**
     * Returns true if a given string is a valid appointment date.
     */
    public static boolean isValidAppointment(String test) {
        if (test.equals("-")) {
            return true;
        }

        try {
            LocalDateTime temp = parse(test);
            return temp.toLocalTime().isAfter(LocalTime.of(8, 29))
                    && temp.toLocalTime().isBefore(LocalTime.of(21, 31));

        } catch (TimeParseException e) {
            return false;
        }
    }

    /**
     * Returns true if a given date is a today.
     */
    public static boolean isToday(String test) {
        return LocalDateTime.parse(test, ENGLISH_FORMAT)
                .toLocalDate().isEqual(TODAY);
    }

    /**
     * Returns true if a given date has passed.
     */
    public static boolean hasPassed(String test) {
        return LocalDateTime.parse(test, ENGLISH_FORMAT)
                .toLocalDate().isAfter(TODAY);
    }

    /**
     * Returns true if a given date in the future.
     */
    public static boolean hasNotPassed(String test) {
        return LocalDateTime.parse(test, ENGLISH_FORMAT)
                .toLocalDate().isBefore(TODAY);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Appointment)) {
            return false;
        }

        Appointment otherAppointment = (Appointment) other;
        return dateTime.equals(otherAppointment.dateTime);
    }

    @Override
    public int hashCode() {
        return dateTime.hashCode();
    }

    /**
     * Format state as text for viewing.
     */
    public String toString() {
        return dateTime;
    }
}
