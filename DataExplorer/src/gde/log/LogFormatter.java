package osde.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * @author brueg
 *
 */
public class LogFormatter extends Formatter {

	//private static final DateFormat	format	= new SimpleDateFormat("HH:mm:ss:SSS");
	private static final String lineSep = System.getProperty("line.separator");
	//private static final DateFormat tf = new SimpleDateFormat("HH:mm:ss:SSS");
	Date dat = new Date();
  private final static String format = "{0,date,yyyy-MM-dd HH:mm:ss.SSS} {1,number,000000} {2}.{3}() - {4}" + lineSep;
  private Object args[] = new Object[5];
  private MessageFormat formatter;
	/*
	SEVERE  	 The highest value; intended for extremely important messages (e.g. fatal program errors).
	WARNING 	Intended for warning messages.
	INFO 	Informational runtime messages.
	CONFIG 	Informational messages about configuration settings/setup.
	FINE 	Used for greater detail, when debugging/diagnosing problems.
	FINER 	Even greater detail.
	FINEST 	The lowest value; greatest detail.
	 */

	/**
	 * A Custom format implementation that is designed for brevity.
	 */
	public synchronized String format(LogRecord record) {
		//StringBuilder sb = new StringBuilder();
		// Minimize memory allocations here.
		dat.setTime(record.getMillis());
		args[0] = dat;
		args[1] = record.getThreadID();
		args[2] = record.getLoggerName();
		args[3] = record.getSourceMethodName();
		args[4] = record.getMessage();
		StringBuffer text = new StringBuffer();
		if (formatter == null) {
		    formatter = new MessageFormat(format);
		}
		formatter.format(args, text, null);
		//sb.append(text).append(" ").append(tf.format(dat)).append(" ");
		//sb.append(String.format(" %06X ", ));
		//sb.append(record.getLoggerName()).append('.');
		//sb.append(record.getSourceMethodName()).append("() - ");
		//sb.append(record.getMessage()).append(lineSep);
		if (record.getThrown() != null) {
			try {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				record.getThrown().printStackTrace(pw);
				pw.close();
				text.append(sw.toString());
			}
			catch (Exception ex) {
			}
		}
		return text.toString();
	}

}
