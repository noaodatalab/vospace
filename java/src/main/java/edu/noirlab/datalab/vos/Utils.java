package edu.noirlab.datalab.vos;

import org.apache.log4j.Logger;

/**
 * Generic wrapper class.
 * Currently to help/easy adding logging statements and stack trace.
 */
public class Utils {
    public static void log_error(Logger log, String msg, Exception e) {
        String excpName = e.getClass().getName();
        log.error(excpName + " " + msg + " : " + e.getLocalizedMessage());
        log.error(getTrace(e.getStackTrace()));
    }

    public static void log_error(Logger log, Exception e) {
        String excpName = e.getClass().getName();
        log.error(excpName + e.getLocalizedMessage());
        log.error(getTrace(e.getStackTrace()));
    }

    public static void log_warn(Logger log, String msg, Exception e) {
        String excpName = e.getClass().getName();
        log.warn(excpName + " " + msg + " : " + e.getLocalizedMessage());
        log.warn(getTrace(e.getStackTrace()));
    }

    public static String getTrace(StackTraceElement[] e) {
        StringBuffer trace = new StringBuffer();
        for (int i = 0; i < e.length && i < 9; i++) {
            trace.append(e[i].toString() + "\n");
        }

        return trace.toString();
    }
}
