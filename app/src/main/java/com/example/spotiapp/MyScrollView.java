package com.example.spotiapp;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.view.Gravity;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class MyScrollView extends ScrollView {
    public MyScrollView(Context context) {
        super(context);
    }

    protected ScrollView makeCenterView() {

        List<String> items = new ArrayList<>();
        for (int i=0; i<50; ++i){
            items.add("test");
        }
        Context activity = getContext();


        ScrollView scrollView = new ScrollView(activity);
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        for (String item : items) {
            LinearLayout line = new LinearLayout(activity);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setGravity(Gravity.CENTER);
            TextView textView = new TextView(activity);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1.0f);
            lp.gravity = Gravity.CENTER;
            textView.setLayoutParams(lp);
            textView.setText(item);
            textView.setGravity(Gravity.CENTER);
            line.addView(textView);
            CheckBox checkBox = new CheckBox(activity);
            checkBox.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP_CONTENT, 0.4f));
            line.addView(checkBox);
            layout.addView(line);
        }
        scrollView.addView(layout);
        return scrollView;
    }
}
