package eu.lastviking.app.vgtd;

import java.util.List;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.Spinner;


public class MultiSpinner extends Spinner implements OnMultiChoiceClickListener {

	public interface Data {
		// We relay on toString() to get the visual data
		public String toString();
		public boolean GetSelected();
		public void SetSelected(boolean selected);
	}
	
	private List<Data> data_;
	private String all_text_;
	private String none_text_;
	private MultiSpinnerListener listener_;
	private boolean[] selected_ = new boolean[0];
	
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
	
	void ClearSelections() {
		for(int i = 0; i < selected_.length; ++i) {
			selected_[i] = false;
		}
		
		SetAdapter();
	}
	
	public int GetNumSelected() {
		int cnt = 0;
		for(int i = 0; i < selected_.length; ++i) {
			if (selected_[i])
				++cnt;
		}
		
		return cnt;
	}
	
	public boolean HasSelected() {
		for(int i = 0; i < selected_.length; ++i) {
			if (selected_[i])
				return true;
		}
		return false;
	}

	@Override
	public boolean performClick() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		CharSequence[] items = new CharSequence[data_.size()];
		
		for(int i = 0; i < selected_.length; ++i) {
			Data d = data_.get(i);
			items[i] = d.toString();
			selected_[i] = d.GetSelected();
		}
		
		builder.setMultiChoiceItems(items, selected_, this);
		builder.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				UpdateData();
				if (null != listener_) {
					listener_.onItemsSelected(selected_);
				}
				dialog.dismiss();
				SetAdapter();
			}
		});
		builder.setNegativeButton(android.R.string.cancel,
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.show();
		return true;
	}
	
	public void SetItems(List<Data> new_items, String allText, String noneText, MultiSpinnerListener listener) {
		
		data_ = new_items;
		none_text_ = noneText;
		all_text_ = allText;
		listener_ = listener;

		selected_ = new boolean[new_items.size()];
		for (int i = 0; i < selected_.length; i++) {
			selected_[i] = data_.get(i).GetSelected();
		}
		
		SetAdapter();
	}
	
	private void SetAdapter() {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
				android.R.layout.simple_spinner_item, new String[] { GetSpinnerText() });
		setAdapter(adapter);
	}
	
	// Must be called when we are done
	public void UpdateData() {
		for (int i = 0; i < selected_.length; i++) {
			data_.get(i).SetSelected(selected_[i]);
		}
	}

	public interface MultiSpinnerListener {
		public void onItemsSelected(boolean[] selected);
	}

	public String GetSpinnerText() {
		StringBuffer spinnerBuffer = new StringBuffer();
		int num_selected = 0;
		for (int i = 0; i < selected_.length; i++) {
			if (selected_[i] == true) {
				if (0 != num_selected++) {
					spinnerBuffer.append(", ");
				}
				spinnerBuffer.append(data_.get(i).toString());
			}
		}
		if (num_selected == selected_.length) {
			return all_text_;
		}
		if (num_selected == 0) {
			return none_text_;
		}
		return spinnerBuffer.toString();
	}
}


