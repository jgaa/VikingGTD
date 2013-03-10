package eu.lastviking.app.vgtd;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;
import java.util.TimeZone;

import eu.lastviking.app.vgtd.R;

import android.renderscript.Type;
import android.util.Log;

class When extends Object implements Serializable 
{
	private static final String TAG = "When";	
	private static final long serialVersionUID = 1L;
	private Calendar time_; // Allows for alarm / reminder (date and time)
	private DueTypes type_ = DueTypes.NONE;
	private final String date_format_ = new String("E, MMMM dd, yy");
	
	class NoDateException extends Exception {
		private static final long serialVersionUID = 1L;
		NoDateException(String msg) {
			super(msg);
		}
	}
	
	When() {}
	
	When(final long ms, final DueTypes type) {
		SetTime(ms, type);
	}
	
	When(final long ms, final int type) {
		SetTime(ms, type);
	}
	
	
	public enum DueTypes {
		NONE,
		YEAR,
		MONTH,
		WEEK,
		DATE,
		TIME
	};
	
	public enum State {
		UNASSIGNED,
		LOOSELY_DEFINED_FUTURE,
		FUTURE,
		TODAY,
		TOMORROW,
		THIS_WEEK,
		IN_LOOSELY_DEFINED_PERIOD,
		OVERDUE
	}
	
	public boolean IsAtDateOrTime() {
		return (type_ == DueTypes.DATE) || (type_ == DueTypes.TIME);
	}
	
	private boolean CompareDate(final Calendar cc) {
		return (cc.get(Calendar.YEAR) == time_.get(Calendar.YEAR))
				&& (cc.get(Calendar.MONTH) == time_.get(Calendar.MONTH))
				&& (cc.get(Calendar.DAY_OF_MONTH) == time_.get(Calendar.DAY_OF_MONTH));
	}
	
	private boolean CompareWeek(final Calendar cc) {
		return (cc.get(Calendar.YEAR) == time_.get(Calendar.YEAR))
				&& (cc.get(Calendar.WEEK_OF_YEAR) == time_.get(Calendar.WEEK_OF_YEAR));
	}
	
	private void SetToDay(Calendar c) {
		c.set(Calendar.HOUR, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
	}
	
	public State GetState() {
		State s = State.UNASSIGNED;

		if (DueTypes.NONE != type_) {
			Calendar cc = new GregorianCalendar();
			cc.setTime(Calendar.getInstance().getTime());

			if (CompareDate(cc))
				s = State.TODAY;
			else if (time_.getTimeInMillis() < Calendar.getInstance().getTimeInMillis()) {
				Calendar end_period = (Calendar)time_.clone(); 
				if (DueTypes.WEEK == type_) {
					SetToDay(end_period);
					end_period.set(Calendar.DAY_OF_WEEK, end_period.getFirstDayOfWeek());
					end_period.add(Calendar.WEEK_OF_YEAR, 1);
				} else if (DueTypes.MONTH == type_) {
					SetToDay(end_period);
					end_period.set(Calendar.DAY_OF_MONTH, end_period.getFirstDayOfWeek());
					end_period.add(Calendar.MONTH, 1);
				} else if (DueTypes.YEAR == type_) {
					SetToDay(end_period);
					end_period.set(Calendar.DAY_OF_YEAR, end_period.getFirstDayOfWeek());
					end_period.add(Calendar.YEAR, 1);
				} 
				if (end_period.getTimeInMillis() > Calendar.getInstance().getTimeInMillis()) {
					s = State.IN_LOOSELY_DEFINED_PERIOD;
				} else {
					s = State.OVERDUE;
				}
			} else {
				cc.add(Calendar.DAY_OF_MONTH, 1);
				if (CompareDate(cc)) {
					s = State.TOMORROW;
				} else {
					if (CompareWeek(Calendar.getInstance())) {
						s = State.THIS_WEEK;
					} else {
						if ((DueTypes.YEAR == type_) || (DueTypes.MONTH == type_) || (DueTypes.WEEK == type_)) {
							s = State.LOOSELY_DEFINED_FUTURE;
						} else {
							s = State.FUTURE;
						}
					}
				}
			}
		}

		return s;
	}

	@Override
	public String toString() {
		try {
			switch (type_) {
			case TIME:
				return getDateAsString() + " " + getTimeAsString();
			case DATE:
				return getDateAsString();
			case WEEK:
				return LastVikingGTD.GetInstance().getString(R.string.week) + " " 
				+ time_.get(Calendar.WEEK_OF_YEAR) + " " + time_.get(Calendar.YEAR);
			case MONTH:
				return getMonthAsString() + " " + getYearAsString(); 
			case YEAR:
				return getYearAsString();
			case NONE:
				;
			}
		} catch(NoDateException ex) {
			Log.e(TAG, "Caught NoDateException: " + ex.getMessage());
			return ex.getMessage();
		}

		return LastVikingGTD.GetInstance().getString(R.string.not_set); // Blank
	}
	
	String getTimeAsString() throws NoDateException {
		if (type_== DueTypes.TIME) {
			return android.text.format.DateUtils.formatDateTime(LastVikingGTD.GetInstance(), time_.getTimeInMillis(), 
				android.text.format.DateUtils.FORMAT_SHOW_TIME);
		}
		throw new NoDateException("No time is set");
	}
	
	String getDateAsString() throws NoDateException {
		if (type_.ordinal() >= DueTypes.DATE.ordinal()) {
			return (String) android.text.format.DateFormat.format("E ", time_.getTime()) 
					+ (String)android.text.format.DateFormat.getMediumDateFormat(LastVikingGTD.GetInstance()).format(time_.getTime());
		}
		throw new NoDateException("No date is set");
	}
	
	String getMonthAsString() throws NoDateException {
		if (type_.ordinal() >= DueTypes.MONTH.ordinal())
			return LastVikingGTD.GetInstance().getResources().getStringArray(R.array.when_months)[time_.get(Calendar.MONTH) +1];
		throw new NoDateException("No month is set");
	}
	
	String getYearAsString() throws NoDateException {
		if (type_.ordinal() >= DueTypes.YEAR.ordinal())
			return Integer.toString(time_.get(Calendar.YEAR));
		throw new NoDateException("No year is set");
	}
	
	DueTypes getDueType() {
		return type_;
	}
		
	Date GetDate() throws NoDateException {
		if (type_ == DueTypes.NONE)
			throw new NoDateException("No date is set");
		return time_.getTime();
	}
	
	public void SetTime(final long ms, final int type) throws IllegalArgumentException {
		DueTypes dt = DueTypes.NONE; 

		switch(type) {
		case 0:
			dt = DueTypes.NONE;
			break;
		case 1:
			dt = DueTypes.YEAR;
			break;
		case 2:
			dt = DueTypes.MONTH;
			break;
		case 3:
			dt = DueTypes.WEEK;
			break;
		case 4:
			dt = DueTypes.DATE;
			break;
		case 5:
			dt = DueTypes.TIME;
			break;
		default:
			Log.e(TAG, "When(): Invalid time type: " + type);
			throw new IllegalArgumentException(Integer.toString(type));
		}
		SetTime(ms, dt);
	}
	
	public void SetTime(final long ms, final DueTypes type) {
		
		type_ = type;
		if (DueTypes.NONE == type) {
			time_ = null;
		} else {
			if (null == time_) {
				time_ = new GregorianCalendar();
			}
			time_.setTimeInMillis(ms);
			
			switch(type_) {
			case YEAR:
				time_.set(Calendar.MONTH, Calendar.JANUARY);
			case MONTH:
				time_.set(Calendar.DAY_OF_MONTH, 1);
			case WEEK:
				if (type == DueTypes.WEEK) {
					time_.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
				}
			case DATE:
				time_.set(Calendar.HOUR_OF_DAY, 0);
				time_.set(Calendar.MINUTE, 0);
			case TIME:
				time_.set(Calendar.SECOND, 0);
				time_.set(Calendar.MILLISECOND, 0);
			case NONE:
				;
			}
		}
	}
	
	public Calendar GetDateOrNow() {
		if (null == time_)
			return Calendar.getInstance();
		return time_;
	}
	
	public long GetAsMilliseconds() {
		if (null != time_) 
			return time_.getTimeInMillis();
		return 0;
	}
	
	// Return a value suitable for sqlite
	public long GetUnixTime() {
		if (null != time_) 
			return time_.getTimeInMillis() / 1000;
		return 0;
	}
}

