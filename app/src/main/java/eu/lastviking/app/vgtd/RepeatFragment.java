package eu.lastviking.app.vgtd;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.GregorianCalendar;

import java.io.Serializable;

public class RepeatFragment extends Fragment {
	
	String TAG = "RepeatFragment";

    boolean cancel_flag_ = false;
    boolean saved_ = false;

    Spinner mode_;
    Spinner when_;
    Spinner unit_;
    View after_controls_;
    NumberPicker units_value_;
    final static int[] unit_max = { 90, 52, 12, 10} ;
    String[] unit_names_;
    TextView unit_name_;
    View repeat_day_view_;
    LinearLayout repeat_days_cont_;
    boolean monday_is_start_of_week_;

	RepeatData data_ = new RepeatData();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        Intent intent = getActivity().getIntent();
        Bundle b = intent.getExtras();
        if (null != b) {
            RepeatData r = (RepeatData) b.getSerializable("repeat");
            if (null != r) {
                data_ = r;
            }
        }
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View view;
		try {
			view = inflater.inflate(R.layout.repeat_fragment, container, false);
		} catch(Exception ex) {
			Log.e(TAG, "Failed to inflate the view: " + ex.getMessage());
			return null;
		}

        unit_names_ = getResources().getStringArray(R.array.repeat_unit_types);

        setHasOptionsMenu(true);

        mode_ = (Spinner)view.findViewById(R.id.repeat_mode);
        unit_ = (Spinner)view.findViewById(R.id.repeat_unit);
        when_ = (Spinner)view.findViewById(R.id.repeat_when);
        after_controls_ = view.findViewById(R.id.repeat_after_unit);
        units_value_ = (NumberPicker)view.findViewById(R.id.repeat_units_value);
        units_value_.setMinValue(1);
        unit_name_ = (TextView)view.findViewById(R.id.repeat_unit_name);
        repeat_day_view_ = view.findViewById(R.id.repeat_day_view);
        repeat_days_cont_ = (LinearLayout)view.findViewById(R.id.repeat_days_cont);

        GregorianCalendar cal = new GregorianCalendar();
        monday_is_start_of_week_ = cal.getFirstDayOfWeek() == GregorianCalendar.MONDAY;

        {
            String[] day_names = getResources().getStringArray(R.array.day_names);
            if (!monday_is_start_of_week_) {
                // Sunday is the start of the week
                CheckBox cb = new CheckBox(getActivity());
                cb.setText(day_names[6]);
                cb.setTag(new Integer(6)); // Actual day (bit) number
                repeat_days_cont_.addView(cb);
            }

            final int offset = monday_is_start_of_week_ ? 0 : 1;
            int align = 0;

            for (int day = 0; day < day_names.length ; ++day) {

                if (!monday_is_start_of_week_ && (day == 6))
                    continue; // Skip sunday, it's already added

                if (day == 7)
                    align = 3; // First special day is 10th bit

                CheckBox cb = new CheckBox(getActivity());
                cb.setText(day_names[day]);
                cb.setTag(new Integer(day + align)); // Actual day (bit) number
                repeat_days_cont_.addView(cb);
            }
        }

        mode_.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != data_.mode_) {
                    data_.mode_ = position;
                    SetViewSelections();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        when_.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != data_.when_) {
                    data_.when_ = position;
                    if (data_.when_ == 0)
                        data_.num_units_ = 1;
                    SetViewSelections();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        unit_.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != data_.unit_) {
                    data_.setUnit(position);
                    SetViewSelections();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        //SetViewSelections();

		return view;
	}

    @Override
    public void onResume() {
        SetViewSelections();
        super.onResume();
    }

	private void SetViewSelections() {
        boolean enabled = true;
        mode_.setSelection(data_.mode_);
        if (data_.mode_ == 0) {
            enabled = false;
        }

        if (data_.when_ == 1) {
            data_.unit_ = 0;
        }

        when_.setSelection(data_.when_);
        when_.setEnabled(enabled);

        unit_.setSelection(data_.unit_);
        unit_.setEnabled(enabled && (data_.when_ == 0));

        after_controls_.setVisibility(((data_.when_ == 0) && (data_.mode_ > 0))
                ? View.VISIBLE : View.GONE);
        units_value_.setMaxValue(unit_max[data_.unit_]);
        unit_name_.setText(unit_names_[data_.unit_]);
        if (data_.when_ == 0) {
            units_value_.setValue((data_.num_units_ > 0) ? data_.num_units_ : 1);
        }

        repeat_day_view_.setVisibility(((data_.when_ == 1)
                && (data_.mode_ > 0)
                && (data_.unit_ == 0))
                ? View.VISIBLE : View.GONE);

        if (data_.when_ == 1) {
            // Set the checked status in the day's based on the bits in the unit value
            for(int i = 0; i < repeat_days_cont_.getChildCount(); ++i) {
                CheckBox cb = (CheckBox) repeat_days_cont_.getChildAt(i);
                Integer bit = (Integer) cb.getTag();
                cb.setChecked((data_.num_units_ & (1 << bit.intValue())) != 0);
            }
        }
	}

    public void SaveResult() {

        if (!cancel_flag_ && !saved_) {
            Intent data = new Intent();
            Bundle b = new Bundle();

            if (data_.when_ == 1) {
                // Get the bits in the unit-value based on the checked days
                data_.num_units_ = 0;
                for(int i = 0; i < repeat_days_cont_.getChildCount(); ++i) {
                    CheckBox cb = (CheckBox)repeat_days_cont_.getChildAt(i);
                    if (cb.isChecked()) {
                        Integer bit = (Integer)cb.getTag();
                        data_.num_units_ |= (1 << bit.intValue());
                    }
                }
            } else {
                data_.num_units_ = units_value_.getValue();
            }

            b.putSerializable("repeat", data_);
            data.putExtras(b);

            getActivity().setResult(EditActionFragment.RESULT_OK, data);
            saved_ = true;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.repeat, menu);
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
