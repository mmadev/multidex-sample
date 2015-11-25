package com.github.mmadev.multidexsample;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.github.mmadev.multidexsample.utils.MultiDexUtils;

import java.util.List;

public class MainActivity extends ListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            final List externalClasses = new MultiDexUtils().getExternalDexClasses(this);
            if(externalClasses != null) {
                final TextView header = new TextView(this);
                header.setText(R.string.header);
                getListView().addHeaderView(header);
                getListView().setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, externalClasses));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
