package eu.lastviking.app.vgtd;

import java.util.Calendar;
import java.util.GregorianCalendar;

import eu.lastviking.app.vgtd.R;
import eu.lastviking.app.vgtd.When;
import eu.lastviking.app.vgtd.When.DueTypes;
import eu.lastviking.app.vgtd.When.NoDateException;

import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.app.TimePickerDialog;
import android.app.DatePickerDialog;

public class WhenFragment extends Fragment {
	
	private static final String TAG = "WhenFragment"; 
	private static final int DATE_DIALOG_ID = 1;
	private static final int YEAR_OFFSET = 2010;
	
	VikingDropdownCtl when_shortcuts_ctl_;
	VikingDropdownCtl when_year_ctl_;
	VikingDropdownCtl when_month_ctl_;
	Button when_date_ctl_;
	Button when_calender_ctl_;
	Button when_time_ctl_;
	TextView when_text_ctl_;
	When when_;
	private boolean cancel_flag_ = false;
	private boolean saved_ = false;
	
	void Done() {
		try {
			SaveResult();
			getActivity().finish();
		} catch (Throwable e) {
			;
		}
	}
		
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		Intent intent = getActivity().getIntent();
		Bundle b = intent.getExtras();
		if (null != b) {
			When w = (When) b.getSerializable("when");
			if (null != w) {
				when_ = w;
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View view;
		try {
			view = inflater.inflate(R.layout.when_fragment, container, false);
		} catch(Exception ex) {
			Log.e(TAG, "Failed to inflate the view: " + ex.getMessage());
			//throw ex;
			return null;
		}
		
		setHasOptionsMenu(true);
		
		when_shortcuts_ctl_ = (VikingDropdownCtl)view.findViewById(R.id.when_shortcuts);
		when_shortcuts_ctl_.SetData(R.array.when_shortcuts, 0, R.string.shortcuts);
		
		when_year_ctl_ = (VikingDropdownCtl)view.findViewById(R.id.when_year);
		when_year_ctl_.SetData(R.array.when_years, -1, R.string.year);
		
		when_month_ctl_ = (VikingDropdownCtl)view.findViewById(R.id.when_month);
		when_month_ctl_.SetData(R.array.when_months, -1, R.string.month);
		
		when_date_ctl_ = (Button)view.findViewById(R.id.when_date);
		when_calender_ctl_ = (Button)view.findViewById(R.id.when_calender);
		when_time_ctl_ = (Button)view.findViewById(R.id.when_time);
		
		when_text_ctl_ = (TextView)view.findViewById(R.id.when_text);

		when_shortcuts_ctl_.SetEventsHandler(new VikingDropdownCtl.Events()  {

			@Override
			public void OnItemSelected(Object selectedItem, final int index) {

				Calendar c = new GregorianCalendar();
				c.setTime(Calendar.getInstance().getTime());

				switch(index) {
				case 0: // Label - ignore
					return;
				case 1: // today
					when_.SetTime(c.getTimeInMillis(), When.DueTypes.DATE);
					break;
				case 2: // tomorrow
					c.add(Calendar.DAY_OF_MONTH, 1);
					when_.SetTime(c.getTimeInMillis(), When.DueTypes.DATE);
					break;
				case 3: /* next Monday */ {
						if ((c.getFirstDayOfWeek() == Calendar.SUNDAY) 
								&& (c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)) {
							; // Skip move to next week
						} else {
							c.add(Calendar.WEEK_OF_YEAR, 1);
						}
						c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
						when_.SetTime(c.getTimeInMillis(), When.DueTypes.DATE);
					}
					break;
				case 4: // This week
					c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
					when_.SetTime(c.getTimeInMillis(), When.DueTypes.WEEK);
					break;
				case 5: // After one week
					c.add(Calendar.WEEK_OF_YEAR, 1);
					when_.SetTime(c.getTimeInMillis(), When.DueTypes.DATE);
					break;
				case 6: // Next week
					c.add(Calendar.WEEK_OF_YEAR, 1);
					c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
					when_.SetTime(c.getTimeInMillis(), When.DueTypes.WEEK);
					break;
				case 7: // This month
					c.set(Calendar.DAY_OF_MONTH, 1);
					when_.SetTime(c.getTimeInMillis(), When.DueTypes.MONTH);
					break;
				case 8: // Next month
					c.set(Calendar.DAY_OF_MONTH, 1);
					c.add(Calendar.MONTH, 1);
					when_.SetTime(c.getTimeInMillis(), When.DueTypes.MONTH);
					break;
				case 9: /* Next quarter */ {
						final int month = c.get(Calendar.MONTH);
						int offset = 4 - (month % 4);
						c.add(Calendar.MONTH, offset);
						when_.SetTime(c.getTimeInMillis(), When.DueTypes.MONTH);
					}
					break;
				case 10: // Next year
					c.add(Calendar.YEAR, 1);
					when_.SetTime(c.getTimeInMillis(), When.DueTypes.YEAR);
					break;
				case 11: // Not set
					when_.SetTime(0, When.DueTypes.NONE);
					break;
				}

				SetAllFields();
				Done();
			}
		});
		

		when_year_ctl_.SetEventsHandler(new VikingDropdownCtl.Events()  {

			@Override
			public void OnItemSelected(Object selectedItem, final int index) {

				final int year = (int)index + YEAR_OFFSET;
				if (YEAR_OFFSET < year) {
					Calendar c = when_.GetDateOrNow();
					c.set(Calendar.YEAR, year);
					Log.d(TAG, "Setting year " + year);
					when_.SetTime(c.getTimeInMillis(), When.DueTypes.YEAR);
				} else {
					when_.SetTime(0, When.DueTypes.NONE);
				}
				SetAllFields();
			}
	
		});

		
		when_month_ctl_.SetEventsHandler(new VikingDropdownCtl.Events()  {
			@Override
			public void OnItemSelected(Object selectedItem, final int index) {

				Calendar c = when_.GetDateOrNow();
				final int month = (int)index -1;
				if (0 <= month) {
					c.set(Calendar.MONTH, (int)month);
					when_.SetTime(c.getTimeInMillis(), When.DueTypes.MONTH);
					Log.d(TAG, "Setting month " + month);
				} else if (-1 == month) {
					when_.SetTime(c.getTimeInMillis(), When.DueTypes.YEAR);
				}
				SetAllFields();
			}
		});
		
		when_date_ctl_.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Calendar c = when_.GetDateOrNow();
				
				DatePickerDialog d = new DatePickerDialog(getActivity(), 
						new  DatePickerDialog.OnDateSetListener() {

							@Override
							public void onDateSet(DatePicker view, int year,
									int monthOfYear, int dayOfMonth) {
								
							}
					
				},  c.get(Calendar.YEAR),  c.get(Calendar.MONTH),  c.get(Calendar.DAY_OF_MONTH));
				d.setCancelable(true);
				d.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int which) {
				       if (which == DialogInterface.BUTTON_NEGATIVE) {
				          dialog.cancel();
				       }
				    }});
				d.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.save), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (which == DialogInterface.BUTTON_POSITIVE) {
							Calendar nc = when_.GetDateOrNow();
							
							Log.d(TAG, "Setting date");
							
							DatePicker dp = ((DatePickerDialog)dialog).getDatePicker();
							nc.set(Calendar.YEAR, dp.getYear());
							nc.set(Calendar.MONTH, dp.getMonth());
							nc.set(Calendar.DAY_OF_MONTH, dp.getDayOfMonth());

							if (when_.getDueType() == DueTypes.TIME)
								when_.SetTime(nc.getTimeInMillis(), When.DueTypes.TIME);
							else
								when_.SetTime(nc.getTimeInMillis(), When.DueTypes.DATE);
							SetAllFields();
							dialog.dismiss();
						}
					}});
				d.show();
			}

		});
		
		when_time_ctl_.setOnClickListener(new OnClickListener() {
			
			boolean cancelled_;
			
			@Override
			public void onClick(View v) {
				Calendar c = new GregorianCalendar();
				c.setTime(when_.GetDateOrNow().getTime());
				
				if (when_.getDueType() != DueTypes.TIME) {
					c.set(Calendar.HOUR_OF_DAY, Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + 1);
					c.set(Calendar.MINUTE, 0);
				}
				
				cancelled_ = false;

				TimePickerDialog t = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {

					@Override
					public void onTimeSet(TimePicker view, int hourOfDay,
							int minute) {

						if (!cancelled_) {

							Calendar nc = when_.GetDateOrNow();
							nc.set(Calendar.HOUR_OF_DAY, hourOfDay);
							nc.set(Calendar.MINUTE, minute);

							when_.SetTime(nc.getTimeInMillis(), When.DueTypes.TIME);
							SetAllFields();

							Log.d(TAG, "Setting time: " + hourOfDay + ":" + minute);
						} else {
							Log.d(TAG, "Setting time: **cancelled**");
						}
					}
					
				}, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true);
				
				t.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int which) {
				       if (which == DialogInterface.BUTTON_NEGATIVE) {
				    	   cancelled_ = true;
				    	   dialog.cancel();
				       }
				    }});
				t.show();
			}

		});
		
		return view;
	}
	

	@Override
	public void onResume() {
		SetAllFields();
		super.onResume();
	}

	private void SetAllFields() {

		when_text_ctl_.setText(when_.toString());
		
		when_shortcuts_ctl_.SetSelected(0); // Always set to the label
		
		When.DueTypes dt = when_.getDueType(); 

		if (When.DueTypes.TIME == dt) {
			try {
				when_time_ctl_.setText(when_.getTimeAsString());
			} catch (NoDateException e1) {
				e1.printStackTrace();
				when_time_ctl_.setText(e1.getMessage());
			}
		} else {
			when_time_ctl_.setText(R.string.time);
		}

		if (When.DueTypes.DATE.ordinal() <= dt.ordinal()) {
			try {
				when_date_ctl_.setText(when_.getDateAsString());
			} catch (NoDateException e) {
				e.printStackTrace();
				when_date_ctl_.setText(e.getMessage());
			}
		} else {
			when_date_ctl_.setText(R.string.date);
		}

		if (When.DueTypes.MONTH.ordinal() <= dt.ordinal()) {
			when_month_ctl_.SetSelected(when_.GetDateOrNow().get(Calendar.MONTH) + 1); // January is 1
		} else {
			when_month_ctl_.SetSelected(-1); // January is 1
		}

		if (When.DueTypes.YEAR.ordinal() <= dt.ordinal()) {
			final int index = when_.GetDateOrNow().get(Calendar.YEAR) - YEAR_OFFSET;
			when_year_ctl_.SetSelected(index);
		} else {
			when_year_ctl_.SetSelected(-1);
		}
	}
	
	
	public void SaveResult() {

		if (!cancel_flag_ && !saved_) {
			Intent data = new Intent();
			Bundle b = new Bundle();
			b.putSerializable("when", when_);
			data.putExtras(b);

			getActivity().setResult(EditActionFragment.RESULT_OK, data);
			saved_ = true;
		}
	}
	
	@Override 
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {        	
		
		inflater.inflate(R.menu.when, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch(item.getItemId()) {
		case R.id.cancel_when:
			cancel_flag_ = true;
			getActivity().finish();
			break;
		case R.id.save_when:
			cancel_flag_ = false;
			SaveResult();
			getActivity().finish();
		}
		
		return super.onOptionsItemSelected(item);
	}
}
