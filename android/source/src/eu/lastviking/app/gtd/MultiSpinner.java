package eu.lastviking.app.gtd;

// Original code from http://stackoverflow.com/questions/5015686/android-spinner-with-multiple-choice
// Modified by jgaa.

import java.util.ArrayList;
import java.util.List;

import eu.lastviking.app.gtd.MultiSpinner.Data;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class MultiSpinner extends Spinner implements
OnMultiChoiceClickListener {

	interface Data {
		// We relay on toString() to get the visual data
		String toString();
		boolean getSelected();
		void setSelected(boolean selected);
	}
	
	private List<Data> data_;
	private List<String> items_;
	private boolean[] selected_;
	private String all_text_;
	private String none_text_;
	private MultiSpinnerListener listener_;
	
	public MultiSpinner(Context context) {
		super(context);
	}

	public MultiSpinner(Context arg0, AttributeSet arg1) {
		super(arg0, arg1);
	}

	public MultiSpinner(Context arg0, AttributeSet arg1, int arg2) {
		super(arg0, arg1, arg2);
	}

	@Override
	public void onClick(DialogInterface dialog, int which, boolean isChecked) {
		selected_[which] = isChecked;
	}


	@Override
	public boolean performClick() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		builder.setMultiChoiceItems(
				items_.toArray(new CharSequence[items_.size()]), selected_, this);
		builder.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		builder.setOnCancelListener( new android.content.DialogInterface.OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				
				ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
						android.R.layout.simple_spinner_item,
						new String[] { getSpinnerText() });
				setAdapter(adapter);

				if (null != listener_) {
					listener_.onItemsSelected(selected_);
				}
				updateData();
			}

		});
		builder.show();
		return true;
	}
	
	public void setItems(List<Data> new_items, String allText, String noneText, MultiSpinnerListener listener) {
		
		data_ = new_items;
		items_ = new ArrayList<String>() ;
		none_text_ = noneText;
		all_text_ = allText;
		listener_ = listener;

		// all selected by default
		selected_ = new boolean[new_items.size()];
		for (int i = 0; i < selected_.length; i++) {
			final Data d = data_.get(i);
			selected_[i] = d.getSelected();
			items_.add(d.toString());
		}

		// all text on the spinner
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
				android.R.layout.simple_spinner_item, new String[] { getSpinnerText() });
		setAdapter(adapter);
	}
	
	// Must be called when we are done
	public void updateData() {
		for (int i = 0; i < selected_.length; i++) {
			data_.get(i).setSelected(selected_[i]);
		}
	}

	public interface MultiSpinnerListener {
		public void onItemsSelected(boolean[] selected);
	}

	
	/*public void doCancel() {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
				android.R.layout.simple_spinner_item,
				new String[] { getSpinnerText() });
		setAdapter(adapter);
		
		if (null != listener) {
			listener.onItemsSelected(selected);
		}
		updateData();
	}*/

	public String getSpinnerText() {
		StringBuffer spinnerBuffer = new StringBuffer();
		int num_selected = 0;
		for (int i = 0; i < items_.size(); i++) {
			if (selected_[i] == true) {
				if (0 != num_selected++) {
					spinnerBuffer.append(", ");
				}
				spinnerBuffer.append(items_.get(i));
			}
		}
		if (num_selected == items_.size()) {
			return all_text_;
		}
		if (num_selected == 0) {
			return none_text_;
		}
		return spinnerBuffer.toString();
	}

}


