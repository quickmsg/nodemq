package io.github.quickmsg.edge.mqtt.log;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * @author luxurong
 */
public class CustomFormatter extends Formatter {


    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");


    @Override
    public String format(LogRecord record) {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(
                record.getInstant(), ZoneId.systemDefault());
        var format = zdt.format(formatter);
        return format+" "+record.getLongThreadID() +" [" + record.getLevel() + "] " + record.getMessage() + "\n";
    }
}