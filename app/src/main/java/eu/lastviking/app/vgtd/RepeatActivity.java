package eu.lastviking.app.vgtd;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

/**
 * Created by jgaa on 4/14/14.
 */
public class RepeatActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.repeat_layout);
        this.setTitle(R.string.repeat);
    }

    @Override
    public void onBackPressed() {
        SaveResult();
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        //SaveResult();
        super.onPause();
    }

    void SaveResult() {
        Fragment f = getFragmentManager().findFragmentById(R.id.repeat_fragment_node);
        if (null != f) {
            RepeatFragment r = (RepeatFragment)f;
            r.SaveResult();
        }
    }
}
