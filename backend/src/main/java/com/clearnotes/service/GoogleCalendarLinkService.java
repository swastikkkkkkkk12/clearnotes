package com.clearnotes.service;

import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
public class GoogleCalendarLinkService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    public CalendarLink create(String title, LocalDate date, LocalTime start, LocalTime end, Long segmentId) {
        if (date == null)
            return new CalendarLink(null, "No event link yet: the transcript did not state a specific calendar date.");
        if (start != null && end == null)
            return new CalendarLink(null, "No event link yet: the transcript did not state an end time. Add the meeting duration in Google Calendar.");
        String dates;
        String message;
        if (start == null) {
            dates = date.format(DATE) + "/" + date.plusDays(1).format(DATE);
            message = "All-day event. Review it, set a notification, and save it in Google Calendar.";
        } else {
            LocalDateTime startAt = LocalDateTime.of(date, start);
            LocalDateTime endAt = LocalDateTime.of(date, end);
            dates = startAt.format(DATE_TIME) + "/" + endAt.format(DATE_TIME);
            message = "Review the pre-filled event, set a notification, and save it in Google Calendar.";
        }
        String details = "ClearNotes transcript evidence: segment " + segmentId;
        String url = "https://calendar.google.com/calendar/render?action=TEMPLATE"
                + "&text=" + encode(title) + "&dates=" + encode(dates) + "&details=" + encode(details);
        return new CalendarLink(url, message);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record CalendarLink(String url, String message) { }
}
