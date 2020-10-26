package eu.lastviking.app.vgtd;

import android.app.Application;

import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;


public class RepeatData implements Serializable {
    public int mode_ = 0; // Basic type (0 - 3)
    public int when_ = 0; // When (modifier)
    public int unit_ = 0; // day, week ...
    public int num_units_ = 0; // Depends on the above

    static final public int NO_REPEAT = 0;
    static final public int REPEAT_AFTER_COMPLETED = 1;
    static final public int REPEAT_AFTER_SCHEDULED = 2;
    static final public int REPEAT_AT_DAY = 0x100;

    static final public int UNIT_DAYS = 0;
    static final public int UNIT_WEEKS = 1;
    static final public int UNIT_MONTHS = 2;
    static final public int UNIT_YEARS = 3;

    static final int [] european_days = { 0, 1, 2, 3, 4, 5, 6 };
    static final int [] us_days = { 1, 2, 3, 4, 5, 6, 0 };


    @Override
    public String toString() {
        switch(mode_) {
            case NO_REPEAT: return "No repeat";
            case REPEAT_AFTER_COMPLETED: return "After complete";
            case REPEAT_AFTER_SCHEDULED: return "After scheduled";
        }

        return "Repeat";
    }

    public long getMode() {
        long mode = mode_;
        if (when_ != 0)
            mode |= 0x100;
        return mode;
    }

    public void setMode(long dataMode) throws IllegalArgumentException {
        mode_ = (int)dataMode & 0xff;
        when_ = (((int)dataMode & REPEAT_AT_DAY) == REPEAT_AT_DAY) ? 1 : 0;
    }

    public long getUnit() {
        return unit_;
    }

    public void setUnit(long unit) throws IllegalArgumentException {
        unit_ = (int)unit;
    }

    public long getNumUnits() {
        return num_units_;
    }

    public void setNumUnits(long numUnits) throws IllegalArgumentException {
        num_units_ = (int)numUnits;
    }

    int getDayOfWeek(int javaDay) throws IllegalArgumentException  {
        switch(javaDay) {
            case Calendar.MONDAY: return 0;
            case Calendar.TUESDAY: return 1;
            case Calendar.WEDNESDAY: return 2;
            case Calendar.THURSDAY: return 3;
            case Calendar.FRIDAY: return 4;
            case Calendar.SATURDAY: return 5;
            case Calendar.SUNDAY: return 6;
            default:
                throw new IllegalArgumentException();
        }
    }

    public Calendar Calculate(final Calendar scheduled, final Calendar finished) {
        Calendar c;

        if (mode_ == REPEAT_AFTER_COMPLETED)
            c = (Calendar)finished.clone();
        else
            c = (Calendar)scheduled.clone();

        if (when_ == 0) {
            // Add num_units_ units
            switch(unit_) {
                case UNIT_DAYS:
                    c.add(Calendar.DAY_OF_YEAR, num_units_);
                    break;
                case UNIT_WEEKS:
                    c.add(Calendar.WEEK_OF_YEAR, num_units_);
                    break;
                case UNIT_MONTHS:
                    c.add(Calendar.MONTH, num_units_);
                    break;
                case UNIT_YEARS:
                    c.add(Calendar.YEAR, num_units_);
                    break;
            }
        } else {
            // Search for the next repeat occurrence
            // Find the one closest in time into the future
            Calendar base = (Calendar)Calendar.getInstance().clone();
            base.setTime(c.getTime());
            base.add(Calendar.DAY_OF_YEAR, 1); // Require at least one day into the future
            Calendar best_match = (Calendar)base.clone();
            best_match.add(Calendar.YEAR, 101);
            final int void_year = best_match.get(Calendar.YEAR);
            Calendar probe = (Calendar)best_match.clone();

            int [] days;
            if (base.getFirstDayOfWeek() == Calendar.MONDAY)
                days = european_days;
            else
                days = us_days;

            final int weekday = getDayOfWeek(base.get(Calendar.DAY_OF_WEEK));
            final int base_day = days[weekday];

            for(int day = 0; day < 7; ++day) {
                if (((1 << day) & num_units_) != 0) {
                    probe.setTime(base.getTime());

                    if (days[day] < base_day)
                        probe.add(Calendar.WEEK_OF_YEAR, 1); // Next week

                    if (days[day] != base_day) {
                        probe.add(Calendar.DAY_OF_MONTH, days[day] - base_day);
                    }

                    if (probe.getTimeInMillis() < best_match.getTimeInMillis()) {
                        best_match.setTime(probe.getTime());
                    }
                }
            }

            // Check the special days
            if (((1 << 10) & num_units_) != 0) {
                // first day of week
                probe.setTime(base.getTime());
                probe.add(Calendar.WEEK_OF_YEAR, 1);
                probe.set(Calendar.DAY_OF_WEEK, probe.getFirstDayOfWeek());
                if (probe.getTimeInMillis() < best_match.getTimeInMillis()) {
                    best_match.setTime(probe.getTime());
                }
            }

            if (((1 << 11) & num_units_) != 0) {
                // last day of week
                probe.setTime(base.getTime());
                //probe.add(Calendar.WEEK_OF_YEAR, 1);
                probe.set(Calendar.DAY_OF_WEEK,
                    probe.getFirstDayOfWeek() == Calendar.MONDAY?
                    Calendar.SUNDAY : Calendar.SATURDAY);
                if (probe.getTimeInMillis() < best_match.getTimeInMillis()) {
                    best_match.setTime(probe.getTime());
                }
            }

            if (((1 << 12) & num_units_) != 0) {
                // first day of month
                probe.setTime(base.getTime());
                probe.add(Calendar.MONTH, 1);
                probe.set(Calendar.DAY_OF_MONTH, 1);
                if (probe.getTimeInMillis() < best_match.getTimeInMillis()) {
                    best_match.setTime(probe.getTime());
                }
            }

            if (((1 << 13) & num_units_) != 0) {
                // last day of month
                probe.setTime(base.getTime());
                probe.add(Calendar.MONTH, 1);
                probe.set(Calendar.DAY_OF_MONTH, 1);
                probe.add(Calendar.DAY_OF_YEAR, -1);

                if (probe.getTimeInMillis() < best_match.getTimeInMillis()) {
                    best_match.setTime(probe.getTime());
                }
            }

            if (((1 << 14) & num_units_) != 0) {
                // first day of year
                probe.setTime(base.getTime());
                probe.add(Calendar.YEAR, 1);
                probe.set(Calendar.DAY_OF_YEAR, 1);
                if (probe.getTimeInMillis() < best_match.getTimeInMillis()) {
                    best_match.setTime(probe.getTime());
                }
            }

            if (((1 << 15) & num_units_) != 0) {
                // last day of year
                probe.setTime(base.getTime());
                probe.add(Calendar.YEAR, 1);
                probe.set(Calendar.DAY_OF_YEAR, 1);
                probe.add(Calendar.DAY_OF_YEAR, -1);

                if (probe.getTimeInMillis() < best_match.getTimeInMillis()) {
                    best_match.setTime(probe.getTime());
                }
            }

            if (best_match.get(Calendar.YEAR) == void_year)
                c = null; // No repeat was defined
            else
                c = best_match;
        }

        return c;
    }
}
