package io.github.quickmsg.edge.mqtt.log;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * @author luxurong
 */
public class LogFormatter extends Formatter {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public String format(LogRecord record) {
        StringBuilder builder = new StringBuilder();

        ZonedDateTime zdt = ZonedDateTime.ofInstant(
                record.getInstant(), ZoneId.systemDefault());
        var format = zdt.format(formatter);
        // 添加时间
        builder.append(format).append(" ");
//        // 添加线程 ID
//        builder.append("[").append(record.getLongThreadID()).append("] ");
        // 添加日志级别
        builder.append("[").append(record.getLevel()).append("] ");
        // 添加日志消息
        builder.append(formatMessage(record)).append("\n");
        return builder.toString();
    }
}
