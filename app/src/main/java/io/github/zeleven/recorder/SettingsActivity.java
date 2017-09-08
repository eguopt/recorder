package io.github.zeleven.recorder;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

/**
 * Created by user on 17-8-31.
 */

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.layoutId = R.layout.activity_preferences;
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().
                replace(R.id.container, new SettingsFragment()).commit();
    }
}
