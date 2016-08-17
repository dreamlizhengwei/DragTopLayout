package com.test.dragtoplayout;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity {

	private DragTopLayout drag;
	private ListView lv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		drag = (DragTopLayout) findViewById(R.id.drag);
		
		lv = (ListView) findViewById(R.id.lv);
		drag.setTargetView(lv);


        lv.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return 50;
            }

            @Override
            public Object getItem(int position) {
                return null;
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView t = new TextView(MainActivity.this);
                t.setText("==================== " +position);
                return t;
            }
        });
	}
}
