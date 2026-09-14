package com.example.knowyourmoney;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class AdminActivity extends Activity {

    private static final String ADMIN_UID =
            "uusR0xG7kyNt8mb3mGy8nqe1klr1";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private LinearLayout transactionLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null ||
                !ADMIN_UID.equals(mAuth.getCurrentUser().getUid())) {

            Toast.makeText(
                    this,
                    "Admin access only",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(24, 24, 24, 24);

        TextView title = new TextView(this);
        title.setText("👑 ADMIN PANEL");
        title.setTextSize(26);
        title.setPadding(0, 0, 0, 24);

        mainLayout.addView(title);

        TextView info = new TextView(this);
        info.setText("All Users Transactions");
        info.setTextSize(20);
        info.setPadding(0, 0, 0, 16);

        mainLayout.addView(info);
        Button parentalButton = new Button(this);
parentalButton.setText("👨‍👦 PARENTAL SAFETY");

mainLayout.addView(parentalButton);
        Button appUsageButton = new Button(this);
appUsageButton.setText("📊 APP USAGE");

mainLayout.addView(appUsageButton);

appUsageButton.setOnClickListener(v -> {
    loadAllAppUsage();
});

parentalButton.setOnClickListener(v -> {
    Intent intent =
            new Intent(AdminActivity.this, ParentalSafetyActivity.class);
    startActivity(intent);
});

        transactionLayout = new LinearLayout(this);
        transactionLayout.setOrientation(LinearLayout.VERTICAL);

        mainLayout.addView(transactionLayout);

        setContentView(mainLayout);

        loadAllTransactions();
    }

    private void loadAllTransactions() {

        transactionLayout.removeAllViews();

        TextView loading = new TextView(this);
        loading.setText("Loading transactions...");
        loading.setTextSize(18);

        transactionLayout.addView(loading);

        db.collectionGroup("transactions")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    transactionLayout.removeAllViews();

                    if (querySnapshot.isEmpty()) {

                        TextView empty = new TextView(this);
                        empty.setText("No transactions found.");
                        empty.setTextSize(18);

                        transactionLayout.addView(empty);
                        return;
                    }

                    for (QueryDocumentSnapshot document
                            : querySnapshot) {

                        String type =
                                document.getString("type");

                        Double amount =
                                document.getDouble("amount");

                        String note =
                                document.getString("note");

                        String date =
                                document.getString("date");

                        String paymentMode =
                                document.getString("paymentMode");

                        String userId =
                                document.getReference()
                                        .getParent()
                                        .getParent()
                                        .getId();

                        TextView transaction =
                                new TextView(this);

                        transaction.setText(
                                "User ID: " + userId +
                                "\nType: " + type +
                                "\nAmount: ₹" + amount +
                                "\nNote: " + note +
                                "\nDate: " + date +
                                "\nPayment: " + paymentMode +
                                "\n-------------------------"
                        );

                        transaction.setTextSize(17);
                        transaction.setPadding(
                                0, 16, 0, 16
                        );

                        transactionLayout.addView(
                                transaction
                        );
                    }
                })
                .addOnFailureListener(e -> {

                    transactionLayout.removeAllViews();

                    TextView error = new TextView(this);

                    error.setText(
                            "Failed to load transactions\n" +
                            e.getMessage()
                    );

                    error.setTextSize(17);

                    transactionLayout.addView(error);
                });
    }
    private void loadAllAppUsage() {

    transactionLayout.removeAllViews();

    db.collectionGroup("appUsage")
            .get()
            .addOnSuccessListener(querySnapshot -> {

                transactionLayout.removeAllViews();

                android.content.pm.PackageManager pm =
                        getPackageManager();

                for (QueryDocumentSnapshot document : querySnapshot) {

                    String packageName =
                            document.getString("packageName");

                    Long usageTime =
                            document.getLong("usageTime");

                    if (packageName == null ||
                            usageTime == null ||
                            usageTime <= 0) {
                        continue;
                    }

                    try {
                        android.content.pm.ApplicationInfo appInfo =
                                pm.getApplicationInfo(packageName, 0);

                        if ((appInfo.flags &
                                android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0) {
                            continue;
                        }

                        String appName =
                                pm.getApplicationLabel(appInfo).toString();

                        long minutes =
                                usageTime / (1000 * 60);

                        String usage;

                        if (minutes >= 60) {
                            usage =
                                    (minutes / 60) + "h " +
                                    (minutes % 60) + "m";
                        } else {
                            usage =
                                    minutes + "m";
                        }

                        String userId =
                                document.getReference()
                                        .getParent()
                                        .getParent()
                                        .getId();

                        TextView usageText =
                                new TextView(this);

                        usageText.setText(
                                "👤 User: " + userId +
                                "\n📱 " + appName +
                                " — " + usage
                        );

                        usageText.setTextSize(18);
                        usageText.setPadding(
                                0, 16, 0, 16
                        );

                        transactionLayout.addView(
                                usageText
                        );

                    } catch (Exception ignored) {
                    }
                }
            })
            .addOnFailureListener(e -> {

                transactionLayout.removeAllViews();

                TextView error =
                        new TextView(this);

                error.setText(
                        "Failed to load app usage\n" +
                        e.getMessage()
                );

                error.setTextSize(17);
                transactionLayout.addView(error);
            });
    }
}

    

    
