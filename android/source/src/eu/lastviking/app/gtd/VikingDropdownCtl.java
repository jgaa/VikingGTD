package eu.lastviking.app.gtd;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

public class VikingDropdownCtl extends Button {

	private String label_;
	private ArrayList<Object> items_ = new ArrayList<Object>();
	private int selected_ = -1;
	private Events events_;
	
	public interface Events {
		
		// called when one item is selected
		// If bo object is selected, selectedItem will be null
		void OnItemSelected(Object selectedItem, final int index);		
	}
	
	
	
	public VikingDropdownCtl(Context context) {
		super(context);
		SetClickHandler();
	}
	
	public VikingDropdownCtl(Context context,  AttributeSet attrs) {
		super(context, attrs);
		SetClickHandler();
	}
	
	private void SetClickHandler() {
		this.setOnClickListener( new OnClickListener() {

			@Override
			public void onClick(View v) {
				
				SelectItem();
			}
		});
	}
	
	public void SetEventsHandler(Events events) {
		events_ = events;
	}
	
	// Present a list of items where the user can
	// select one. When selected, the list closes.
	private void SelectItem() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
	    builder.setTitle(label_);
	    
	    CharSequence[] items = new CharSequence[items_.size()];
	    for(int i = 0; i < items_.size(); ++i) {
	    	items[i] = items_.get(i).toString();
	    }
	    
	    builder.setItems(items, new DialogInterface.OnClickListener() {
	    	public void onClick(DialogInterface dialog, int which) {
	    		selected_ = which;
	    		if (null != events_) {
	    			events_.OnItemSelected(HaveSelected() ? GetSelected() : null, selected_);
	    		}
	    	}
	    });
	    
	    builder.show();
	}
	
	public void SetData(ArrayList<Object> items, int selectedItemIndex, final int label) {
		label_ = getContext().getString(label);
		items_ = items;
		selected_ = selectedItemIndex;
		UpdateText();
	}
	
	private void UpdateText() {
		if (-1 == selected_) {
			setText(label_);
		} else {
			setText(GetSelected().toString());
		}
	}
	
	public void SetData(int resourceIdToStringArray, int selectedItemIndex, final int label) {
		String[] strings = getContext().getResources().getStringArray(resourceIdToStringArray);
		ArrayList<Object> my_items = new ArrayList<Object>();
		
		for(int i = 0; i < strings.length; ++i) {
			my_items.add(strings[i]);
		}
		
		SetData(my_items, selectedItemIndex, label);
	}
	
	public void SetSelected(final int index) {		
		selected_ = index;
		UpdateText();
	}
	
	public Object GetSelected() {
		return items_.get(selected_);
	}
	
	// -1 means no selected
	public int GetSelectedIndex() {
		return selected_;
	}
	
	public boolean HaveSelected() {
		return -1 != selected_;
	}
}
