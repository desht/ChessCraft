package me.desht.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("serial")
public class Duration {
	long days, hours, minutes, seconds, milliseconds;
	private static final Map<String,Integer> specMap = new HashMap<String,Integer>() {
		{	
			put("ms", 1);
			put("s", 1000);
			put("m", 60000);
			put("h", 3600000);
			put("d", 86400000);
			put("milliseconds", 1);
			put("seconds", 1000);
			put("minutes", 60000);
			put("hours", 3600000);
			put("days", 86400000);
		}
	};
	
	/**
	 * Create a new Duration object from the given parameters
	 * 
	 * @param d	Days
	 * @param h Hours
	 * @param m Minutes
	 * @param s Seconds
	 */
	public Duration(long d, long h, long m, long s, long ms) {
		days = d;
		hours = h;
		minutes = m;
		seconds = s;
		milliseconds = ms;
		
		Duration dur = new Duration(getTotalDuration());
		days = dur.getDays();
		hours = dur.getHours();
		minutes = dur.getMinutes();
		seconds = dur.getSeconds();
		milliseconds = dur.getMilliseconds();
	}
	
	/**
	 * Create a new Duration object
	 * 
	 * @param duration	Duration in milliseconds
	 */
	public Duration(long duration) {
		if (duration < 0) {
			throw new IllegalArgumentException("duration must be positive");
		}
		days  = TimeUnit.MILLISECONDS.toDays(duration);
		hours = TimeUnit.MILLISECONDS.toHours(duration) - TimeUnit.DAYS.toHours(days);
		minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
		- TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration));
		seconds = TimeUnit.MILLISECONDS.toSeconds(duration)
		- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration));
	}
	
	/**
	 * Create a new Duration object
	 * 
	 * @param spec	Duration specification 
	 */
	public Duration (String spec) {
		String[] fields = spec.toLowerCase().split(" +");
		long total = 0;
		
		if (fields.length > 1) {
			if (fields.length % 2 == 1) {
				throw new IllegalArgumentException("Odd number of parameters in duration specification");
			}

			for (int i = 0; i < fields.length; i += 2) {
				if (!specMap.containsKey(fields[i])) {
					throw new IllegalArgumentException("Unknown duration specifier " + fields[i]);
				}
				long val = Long.parseLong(fields[i + 1]);
				int mult = specMap.get(fields[i]);

				total += val * mult;
			}
		} else {
			total = Long.parseLong(fields[0]) * 1000;
		}
		
		Duration d = new Duration(total);
		days = d.getDays();
		hours = d.getHours();
		minutes = d.getMinutes();
		seconds = d.getSeconds();
		milliseconds = d.getMilliseconds();
	}
	
	public long getDays() {
		return days;
	}

	public long getHours() {
		return hours;
	}

	public long getMinutes() {
		return minutes;
	}

	public long getSeconds() {
		return seconds;
	}
	
	public long getMilliseconds() {
		return milliseconds;
	}

	public long getTotalDuration() {
		return milliseconds + (seconds + minutes * 60 + hours * 3600 + days * 86400) * 1000;
	}
	
	public String toString() {
		if (days == 0 && milliseconds == 0) {
			return String.format("%02d:%02d:%02d", hours, minutes, seconds);
		} else if (days > 0 && milliseconds == 0) {
			return String.format("%dd%02d:%02d:%02d", days, hours, minutes, seconds);
		} else if (days == 0 && milliseconds > 0) {
			return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds);
		} else {  // if (days > 0 && milliseconds > 0)
			return String.format("%dd%02d:%02d:%02d.%03d", days, hours, minutes, seconds, milliseconds);
		} 
	}
}
