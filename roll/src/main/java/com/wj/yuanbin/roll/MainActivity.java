package com.wj.yuanbin.roll;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.wj.yuanbin.rollbar.RollLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yuanbin on 16/8/5.
 */
public class MainActivity extends Activity {
    RollLayout rollLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        List<String> datas = new ArrayList<String>();

        for (int i = 0 ; i < 10 ; i++){
            datas.add("String"+i);
        }
        rollLayout = (RollLayout)findViewById(R.id.roll);
        rollLayout.setAdapter(new RollLayout.RollAdapter<String>(datas) {
            @Override
            public void refreshView(int position, RollLayout.ViewHolder viewHolder) {
                TextView textView = viewHolder.getView(R.id.txt);
                textView.setText(getItem(position));
            }
        });
        rollLayout.setState(RollLayout.START);

    }
}
