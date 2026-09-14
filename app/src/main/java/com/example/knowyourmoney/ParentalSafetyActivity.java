package com.example.knowyourmoney;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ParentalSafetyActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 24, 24, 24);

        TextView title = new TextView(this);
        title.setText("👨‍👦 PARENTAL SAFETY");
        title.setTextSize(26);
        title.setPadding(0, 0, 0, 24);

        layout.addView(title);

        TextView info = new TextView(this);
        info.setText(
                "Child device must approve access before " +
                "screen or media sharing can begin."
        );
        info.setTextSize(18);
        info.setPadding(0, 0, 0, 24);

        layout.addView(info);

        Button screenButton = new Button(this);
        screenButton.setText("📺 REQUEST SCREEN SHARING");

        layout.addView(screenButton);

        Button galleryButton = new Button(this);
        galleryButton.setText("🖼️ SHARE PHOTOS");

        layout.addView(galleryButton);

        Button usageButton = new Button(this);
        usageButton.setText("📊 APP USAGE");

        layout.addView(usageButton);

        Button locationButton = new Button(this);
        locationButton.setText("📍 LOCATION SHARING");

        layout.addView(locationButton);

        setContentView(layout);
    }
}
