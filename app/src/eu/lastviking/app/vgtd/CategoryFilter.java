package eu.lastviking.app.vgtd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import android.os.Bundle;

public class CategoryFilter {

	static final String PROP_LIST_CAT_KEY = "SelectedListCategories";
	
	public static class Data
	{
		final Integer id_;
		final String name_;
		Boolean selected_;
		
		Data(final Integer id, final String name)
		{
			id_ = id;
			name_ = name;
			selected_ = true;
		}
	}
	
	private Map<Integer, Data> f_;
	
	CategoryFilter()
	{
		// TODO: Fetch from content provider
		f_ = new HashMap<Integer, Data>();
		//for(Data d : LastVikingGTD.GetInstance().GetDb().GetListCategories()) {
		//	f_.put(d.id_, d);
		//}
		Data d1 = new Data(1, "Work"), d2 = new Data(2, "Private");
		f_.put(d1.id_, d1);
		f_.put(d2.id_, d2);
	}
	
	void SetSelected(final Integer id, final Boolean enable) {
		Data d = f_.get(id);
		if (null != d) {
			d.selected_ = enable;
		}
	}
	
	Boolean IsSelected(final Integer id) {
		Data d = f_.get(id);
		if (null != d) {
			return d.selected_;
		}
		return false;
	}
	
	void SelectAll(final Boolean selected) {
		for(Data d : f_.values()) {
			d.selected_ = selected;
		}
	}
	
	final Collection<Data> GetAll() {			
		return f_.values();
	}
	
	// Get a comma separated list of selected id's for use in SQL where clause
	public String GetDbFilter() {
		StringBuilder s = new StringBuilder();
		Boolean virgin = true;
		
		for(Data d : f_.values()) {
			if (d.selected_) {
				if (virgin) 
					virgin = false;
				else
					s.append(",");
				s.append(d.id_.toString());
			}
		}
		
		return s.toString();
	}
	
	public void Save(Bundle sis) {
		ArrayList<Integer> all_selected = new ArrayList<Integer>();
		
		for(Data d : f_.values()) {
			if (d.selected_)
				all_selected.add(d.id_);
		}
		
		sis.putIntegerArrayList(PROP_LIST_CAT_KEY, all_selected);
	}
	
	public void Restore(Bundle sis) {
		ArrayList<Integer> all_selected = sis.getIntegerArrayList(PROP_LIST_CAT_KEY);
		if ((null != all_selected) && (all_selected.size() > 0)) {
			SelectAll(false);
			for(Integer id : all_selected) {
				SetSelected(id, true);
			}
		} else {
			SelectAll(true);
		}
	}
	
	// Set all to selected if all is unselected.
	// Return true if we changed something.
	public Boolean SanetyCheck() {
		Boolean do_reset = true;
		
		for(Data d : f_.values()) {
			if (d.selected_) {
				do_reset = false;
				break;
			}
		}
		
		if (do_reset) {
			SelectAll(true);
		}
		
		return do_reset;
	}
}
