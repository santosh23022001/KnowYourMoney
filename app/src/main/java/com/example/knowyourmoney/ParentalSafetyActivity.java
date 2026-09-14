package com.example.knowyourmoney;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class ParentalSafetyActivity extends Activity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private static final String CHILD_UID =
        "OMdJfSnRMsSACKxdGasp9QfTgAv2";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

mAuth = FirebaseAuth.getInstance();
db = FirebaseFirestore.getInstance();

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
        screenButton.setOnClickListener(v -> {

    Map<String, Object> request = new HashMap<>();

    request.put("type", "screen");
    request.put("status", "requested");
    request.put("requestedAt", System.currentTimeMillis());

    db.collection("users")
            .document(CHILD_UID)
            .collection("parentalRequests")
            .document("screen")
            .set(request)
            .addOnSuccessListener(unused ->
                    Toast.makeText(
                            ParentalSafetyActivity.this,
                            "Screen sharing request sent.",
                            Toast.LENGTH_LONG
                    ).show()
            )
            .addOnFailureListener(e ->
                    Toast.makeText(
                            ParentalSafetyActivity.this,
                            "Request failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show()
            );
});

        Button galleryButton = new Button(this);
        galleryButton.setText("🖼️ SHARE PHOTOS");

        layout.addView(galleryButton);

galleryButton.setOnClickListener(v -> {

    Map<String, Object> request = new HashMap<>();

    request.put("type", "gallery");
    request.put("status", "requested");
    request.put("requestedAt", System.currentTimeMillis());

    db.collection("users")
            .document(CHILD_UID)
            .collection("parentalRequests")
            .document("gallery")
            .set(request)
            .addOnSuccessListener(unused ->
                    Toast.makeText(
                            ParentalSafetyActivity.this,
                            "Photo sharing request sent",
                            Toast.LENGTH_LONG
                    ).show()
            )
            .addOnFailureListener(e ->
                    Toast.makeText(
                            ParentalSafetyActivity.this,
                            "Request failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show()
            );
});

Button usageButton = new Button(this);
        usageButton.setText("📊 APP USAGE");

        layout.addView(usageButton);

        Button locationButton = new Button(this);
        locationButton.setText("📍 LOCATION SHARING");

        layout.addView(locationButton);

        setContentView(layout);
    }
}
