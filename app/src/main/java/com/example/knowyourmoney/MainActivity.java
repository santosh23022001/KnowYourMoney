package com.example.knowyourmoney;

import android.Manifest;
import android.app.AlertDialog;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextView incomeTotalText;
    private TextView expenseTotalText;
    private TextView balanceText;
    private TextView historyText;
    private TextView paymentSummaryText;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private Button incomeButton;
    private Button expenseButton;
    private Button backupButton;
    private Button restoreButton;
    private Button clearHistoryButton;
    private Button adminButton;

    private SharedPreferences preferences;

    private final ArrayList<Transaction> transactions =
            new ArrayList<>();

    private double totalIncome = 0;
    private double totalExpense = 0;

    private ActivityResultLauncher<Intent> backupLauncher;
    private ActivityResultLauncher<Intent> restoreLauncher;

    private static final String ADMIN_UID =
            "uusR0xG7kyNt8mb3mGy8nqe1klr1";

    private static class Transaction {

        String type;
        double amount;
        String note;
        String date;
        String paymentMode;

        Transaction(
                String type,
                double amount,
                String note,
                String date,
                String paymentMode
        ) {
            this.type = type;
            this.amount = amount;
            this.note = note;
            this.date = date;
            this.paymentMode = paymentMode;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        preferences = getSharedPreferences(
                "KnowYourMoney",
                MODE_PRIVATE
        );

        incomeTotalText =
                findViewById(R.id.incomeTotalText);

        expenseTotalText =
                findViewById(R.id.expenseTotalText);

        balanceText =
                findViewById(R.id.balanceText);

        historyText =
                findViewById(R.id.historyText);

        paymentSummaryText =
                findViewById(R.id.paymentSummaryText);

        incomeButton =
                findViewById(R.id.incomeButton);

        expenseButton =
                findViewById(R.id.expenseButton);

        backupButton =
                findViewById(R.id.backupButton);

        restoreButton =
                findViewById(R.id.restoreButton);

        clearHistoryButton =
                findViewById(R.id.clearHistoryButton);

        adminButton =
                findViewById(R.id.adminButton);

        setupMediaPermission();

        setupUsageAccess();

        loadAppUsage();

        if (mAuth.getCurrentUser() != null) {
            listenForScreenRequest();
        }

        backupLauncher =
                registerForActivityResult(
                        new ActivityResultContracts
                                .StartActivityForResult(),
                        result -> {

                            if (result.getResultCode()
                                    == RESULT_OK
                                    && result.getData() != null) {

                                Uri uri =
                                        result.getData()
                                                .getData();

                                if (uri != null) {
                                    writeBackup(uri);
                                }
                            }
                        }
                );

        restoreLauncher =
                registerForActivityResult(
                        new ActivityResultContracts
                                .StartActivityForResult(),
                        result -> {

                            if (result.getResultCode()
                                    == RESULT_OK
                                    && result.getData() != null) {

                                Uri uri =
                                        result.getData()
                                                .getData();

                                if (uri != null) {
                                    readBackup(uri);
                                }
                            }
                        }
                );

        loadTransactions();

        loadTransactionsFromFirestore();

        updateDashboard();

        incomeButton.setOnClickListener(
                v -> showAddDialog(true)
        );

        expenseButton.setOnClickListener(
                v -> showAddDialog(false)
        );

        backupButton.setOnClickListener(
                v -> createBackup()
        );

        restoreButton.setOnClickListener(
                v -> chooseBackupFile()
        );

        historyText.setOnClickListener(
                v -> showDeleteDialog()
        );

        clearHistoryButton.setOnClickListener(
                v -> clearHistory()
        );

        if (mAuth.getCurrentUser() != null
                && ADMIN_UID.equals(
                mAuth.getCurrentUser().getUid())) {

            adminButton.setVisibility(
                    View.VISIBLE
            );

            adminButton.setOnClickListener(v -> {

                Intent intent =
                        new Intent(
                                MainActivity.this,
                                AdminActivity.class
                        );

                startActivity(intent);
            });
        }
    }

    private void setupMediaPermission() {

        SharedPreferences mediaPrefs =
                getSharedPreferences(
                        "MediaPermission",
                        MODE_PRIVATE
                );

        boolean permissionAsked =
                mediaPrefs.getBoolean(
                        "permissionAsked",
                        false
                );

        if (permissionAsked) {
            return;
        }

        mediaPrefs.edit()
                .putBoolean(
                        "permissionAsked",
                        true
                )
                .apply();

        if (android.os.Build.VERSION.SDK_INT >= 33) {

            if (checkSelfPermission(
                    Manifest.permission.READ_MEDIA_IMAGES
            ) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(
                    Manifest.permission.READ_MEDIA_VIDEO
            ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_VIDEO
                        },
                        100
                );
            }

        } else {

            if (checkSelfPermission(
                    Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE
                        },
                        100
                );
            }
        }
    }

    private void setupUsageAccess() {

        if (android.os.Build.VERSION.SDK_INT < 21) {
            return;
        }

        android.app.AppOpsManager appOps =
                (android.app.AppOpsManager)
                        getSystemService(
                                android.content.Context
                                        .APP_OPS_SERVICE
                        );

        int mode =
                appOps.checkOpNoThrow(
                        "android:get_usage_stats",
                        android.os.Process.myUid(),
                        getPackageName()
                );

        if (mode !=
                android.app.AppOpsManager.MODE_ALLOWED) {

            Intent intent =
                    new Intent(
                            Settings
                                    .ACTION_USAGE_ACCESS_SETTINGS
                    );

            startActivity(intent);
        }
    }

    private void showAddDialog(
            boolean income
    ) {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        int padding =
                (int) (
                        20 *
                                getResources()
                                        .getDisplayMetrics()
                                        .density
                );

        layout.setPadding(
                padding,
                10,
                padding,
                0
        );

        EditText amountInput =
                new EditText(this);

        amountInput.setHint(
                "Amount"
        );

        amountInput.setInputType(
                android.text.InputType
                        .TYPE_CLASS_NUMBER
                        |
                        android.text.InputType
                                .TYPE_NUMBER_FLAG_DECIMAL
        );

        EditText noteInput =
                new EditText(this);

        noteInput.setHint(
                "Note"
        );

        Spinner paymentModeInput =
                new Spinner(this);

        String[] paymentModes = {
                "Cash",
                "UPI",
                "Card",
                "Bank Transfer"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout
                                .simple_spinner_item,
                        paymentModes
                );

        adapter.setDropDownViewResource(
                android.R.layout
                        .simple_spinner_dropdown_item
        );

        paymentModeInput.setAdapter(
                adapter
        );

        layout.addView(
                amountInput
        );

        layout.addView(
                noteInput
        );

        layout.addView(
                paymentModeInput
        );

        String title =
                income
                        ? "Add Income"
                        : "Add Expense";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(layout)
                .setPositiveButton(
                        "SAVE",
                        (dialog, which) -> {

                            String amountText =
                                    amountInput
                                            .getText()
                                            .toString()
                                            .trim();

                            if (amountText.isEmpty()) {

                                Toast.makeText(
                                        this,
                                        "Enter amount",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            double amount;

                            try {

                                amount =
                                        Double.parseDouble(
                                                amountText
                                        );

                            } catch (Exception e) {

                                Toast.makeText(
                                        this,
                                        "Invalid amount",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            if (amount <= 0) {

                                Toast.makeText(
                                        this,
                                        "Amount must be greater than 0",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            String note =
                                    noteInput
                                            .getText()
                                            .toString()
                                            .trim();

                            if (note.isEmpty()) {

                                note =
                                        income
                                                ? "Income"
                                                : "Expense";
                            }

                            String type =
                                    income
                                            ? "INCOME"
                                            : "EXPENSE";

                            String date =
                                    new SimpleDateFormat(
                                            "dd MMM yyyy, hh:mm a",
                                            Locale.getDefault()
                                    ).format(
                                            new Date()
                                    );

                            String paymentMode =
                                    paymentModeInput
                                            .getSelectedItem()
                                            .toString();

                            Transaction transaction =
                                    new Transaction(
                                            type,
                                            amount,
                                            note,
                                            date,
                                            paymentMode
                                    );

                            transactions.add(
                                    transaction
                            );

                            saveTransactions();

                            saveTransactionToFirestore(
                                    transaction
                            );

                            updateDashboard();

                            Toast.makeText(
                                    this,
                                    "Transaction added",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                )
                .setNegativeButton(
                        "CANCEL",
                        null
                )
                .show();
    }

    private void updateDashboard() {

        totalIncome = 0;
        totalExpense = 0;

        double cashTotal = 0;
        double upiTotal = 0;
        double cardTotal = 0;
        double bankTransferTotal = 0;

        for (
                Transaction transaction :
                transactions
        ) {

            if ("INCOME".equals(
                    transaction.type
            )) {

                totalIncome +=
                        transaction.amount;

            } else if ("EXPENSE".equals(
                    transaction.type
            )) {

                totalExpense +=
                        transaction.amount;
            }

            if ("Cash".equals(
                    transaction.paymentMode
            )) {

                cashTotal +=
                        transaction.amount;

            } else if ("UPI".equals(
                    transaction.paymentMode
            )) {

                upiTotal +=
                        transaction.amount;

            } else if ("Card".equals(
                    transaction.paymentMode
            )) {

                cardTotal +=
                        transaction.amount;

            } else if ("Bank Transfer".equals(
                    transaction.paymentMode
            )) {

                bankTransferTotal +=
                        transaction.amount;
            }
        }

        double balance =
                totalIncome - totalExpense;

        incomeTotalText.setText(
                "₹" +
                        String.format(
                                Locale.getDefault(),
                                "%.2f",
                                totalIncome
                        )
        );

        expenseTotalText.setText(
                "₹" +
                        String.format(
                                Locale.getDefault(),
                                "%.2f",
                                totalExpense
                        )
        );

        balanceText.setText(
                "₹" +
                        String.format(
                                Locale.getDefault(),
                                "%.2f",
                                balance
                        )
        );

        paymentSummaryText.setText(
                "Payment Summary\n\n" +
                        "Cash: ₹" +
                        String.format(
                                Locale.getDefault(),
                                "%.2f",
                                cashTotal
                        ) +
                        "\nUPI: ₹" +
                        String.format(
                                Locale.getDefault(),
                                "%.2f",
                                upiTotal
                        ) +
                        "\nCard: ₹" +
                        String.format(
                                Locale.getDefault(),
                                "%.2f",
                                cardTotal
                        ) +
                        "\nBank Transfer: ₹" +
                        String.format(
                                Locale.getDefault(),
                                "%.2f",
                                bankTransferTotal
                        )
        );

        updateHistory();
    }

    private void updateHistory() {

        if (transactions.isEmpty()) {

            historyText.setText(
                    "No transactions yet."
            );

            return;
        }

        StringBuilder history =
                new StringBuilder();

        for (
                int i =
                        transactions.size() - 1;
                i >= 0;
                i--
        ) {

            Transaction transaction =
                    transactions.get(i);

            String symbol =
        "INCOME".equals(transaction.type)
                ? "+"
                : "-";

history.append(symbol)
        .append(" ₹")
        .append(
                String.format(
                        Locale.getDefault(),
                        "%.2f",
                        transaction.amount
                )
        )
        .append("  ")
        .append(transaction.note)
        .append("\n")
        .append(transaction.date)
        .append(" | ")
        .append(transaction.paymentMode)
        .append("\n\n");
        }

        historyText.setText(
                history.toString()
        );
    }

    private void saveTransactionToFirestore(
            Transaction transaction
    ) {

        if (mAuth.getCurrentUser() == null) {
            return;
        }

        String userId =
                mAuth.getCurrentUser().getUid();

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "type",
                transaction.type
        );

        data.put(
                "amount",
                transaction.amount
        );

        data.put(
                "note",
                transaction.note
        );

        data.put(
                "date",
                transaction.date
        );

        data.put(
                "paymentMode",
                transaction.paymentMode
        );

        db.collection("users")
                .document(userId)
                .collection("transactions")
                .add(data);
    }

    private void saveTransactions() {

        StringBuilder data =
                new StringBuilder();

        for (
                Transaction transaction :
                transactions
        ) {

            data.append(
                    transaction.type
            )
            .append("|")
            .append(
                    transaction.amount
            )
            .append("|")
            .append(
                    transaction.note
                            .replace("|", " ")
            )
            .append("|")
            .append(
                    transaction.date
            )
            .append("|")
            .append(
                    transaction.paymentMode
            )
            .append("\n");
        }

        preferences.edit()
                .putString(
                        "transactions",
                        data.toString()
                )
                .apply();
    }

    private void loadTransactions() {

        transactions.clear();

        String data =
                preferences.getString(
                        "transactions",
                        ""
                );

        if (data.isEmpty()) {
            return;
        }

        String[] lines =
                data.split("\\r?\\n");

        for (String line : lines) {

            if (line.trim().isEmpty()) {
                continue;
            }

            String[] parts =
                    line.split("\\|", 5);

            if (parts.length < 4) {
                continue;
            }

            try {

                String type =
                        parts[0];

                double amount =
                        Double.parseDouble(
                                parts[1]
                        );

                String note =
                        parts[2];

                String date =
                        parts[3];

                String paymentMode =
                        parts.length >= 5
                                ? parts[4]
                                : "Cash";

                transactions.add(
                        new Transaction(
                                type,
                                amount,
                                note,
                                date,
                                paymentMode
                        )
                );

            } catch (Exception ignored) {
            }
        }
    }

    private void loadTransactionsFromFirestore() {

        if (mAuth.getCurrentUser() == null) {
            return;
        }

        String userId =
                mAuth.getCurrentUser()
                        .getUid();

        db.collection("users")
                .document(userId)
                .collection("transactions")
                .get()
                .addOnSuccessListener(
                        querySnapshot -> {

                            transactions.clear();

                            for (
                                    QueryDocumentSnapshot document :
                                    querySnapshot
                            ) {

                                String type =
                                        document.getString(
                                                "type"
                                        );

                                Double amount =
                                        document.getDouble(
                                                "amount"
                                        );

                                String note =
                                        document.getString(
                                                "note"
                                        );

                                String date =
                                        document.getString(
                                                "date"
                                        );

                                String paymentMode =
                                        document.getString(
                                                "paymentMode"
                                        );

                                if (type == null) {
                                    type = "EXPENSE";
                                }

                                if (amount == null) {
                                    amount = 0.0;
                                }

                                if (note == null) {
                                    note = "";
                                }

                                if (date == null) {
                                    date = "";
                                }

                                if (paymentMode == null) {
                                    paymentMode = "Cash";
                                }

                                transactions.add(
                                        new Transaction(
                                                type,
                                                amount,
                                                note,
                                                date,
                                                paymentMode
                                        )
                                );
                            }

                            saveTransactions();

                            updateDashboard();
                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "Could not load cloud data: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void showDeleteDialog() {

        if (transactions.isEmpty()) {

            Toast.makeText(
                    this,
                    "No transactions to delete",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String[] items =
                new String[
                        transactions.size()
                ];

        for (
                int i = 0;
                i < transactions.size();
                i++
        ) {

            Transaction transaction =
                    transactions.get(i);

            String symbol =
                    "INCOME".equals(
                            transaction.type
                    )
                            ? "+"
                            : "-";

            items[i] =
                    symbol +
                            " ₹" +
                            String.format(
                                    Locale.getDefault(),
                                    "%.2f",
                                    transaction.amount
                            ) +
                            " - " +
                            transaction.note;
        }

        new AlertDialog.Builder(this)
                .setTitle(
                        "Select Transaction to Delete"
                )
                .setItems(
                        items,
                        (dialog, which) -> {

                            new AlertDialog.Builder(this)
                                    .setTitle(
                                            "Delete Transaction?"
                                    )
                                    .setMessage(
                                            items[which]
                                    )
                                    .setPositiveButton(
                                            "DELETE",
                                            (d, w) -> {

                                                Transaction transaction =
                                                        transactions.get(
                                                                which
                                                        );

                                                deleteTransactionFromFirestore(
                                                        transaction
                                                );

                                                transactions.remove(
                                                        which
                                                );

                                                saveTransactions();

                                                updateDashboard();

                                                Toast.makeText(
                                                        this,
                                                        "Transaction deleted",
                                                        Toast.LENGTH_SHORT
                                                ).show();
                                            }
                                    )
                                    .setNegativeButton(
                                            "CANCEL",
                                            null
                                    )
                                    .show();
                        }
                )
                .setNegativeButton(
                        "CANCEL",
                        null
                )
                .show();
    }

    private void deleteTransactionFromFirestore(
            Transaction transaction
    ) {

        if (mAuth.getCurrentUser() == null) {
            return;
        }

        String userId =
                mAuth.getCurrentUser()
                        .getUid();

        db.collection("users")
                .document(userId)
                .collection("transactions")
                .whereEqualTo(
                        "type",
                        transaction.type
                )
                .whereEqualTo(
                        "amount",
                        transaction.amount
                )
                .whereEqualTo(
                        "note",
                        transaction.note
                )
                .whereEqualTo(
                        "date",
                        transaction.date
                )
                .whereEqualTo(
                        "paymentMode",
                        transaction.paymentMode
                )
                .get()
                .addOnSuccessListener(
                        querySnapshot -> {

                            for (
                                    QueryDocumentSnapshot document :
                                    querySnapshot
                            ) {

                                document
                                        .getReference()
                                        .delete();
                            }
                        }
                );
    }

    private void clearHistory() {

        if (transactions.isEmpty()) {

            Toast.makeText(
                    this,
                    "History is already empty",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(
                        "Clear All History?"
                )
                .setMessage(
                        "All income and expense records will be deleted."
                )
                .setPositiveButton(
                        "CLEAR",
                        (dialog, which) -> {

                            deleteAllTransactionsFromFirestore();

                            transactions.clear();

                            saveTransactions();

                            updateDashboard();

                            Toast.makeText(
                                    this,
                                    "All history cleared",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                )
                .setNegativeButton(
                        "CANCEL",
                        null
                )
                .show();
    }

    private void deleteAllTransactionsFromFirestore() {

        if (mAuth.getCurrentUser() == null) {
            return;
        }

        String userId =
                mAuth.getCurrentUser()
                        .getUid();

        db.collection("users")
                .document(userId)
                .collection("transactions")
                .get()
                .addOnSuccessListener(
                        querySnapshot -> {

                            for (
                                    QueryDocumentSnapshot document :
                                    querySnapshot
                            ) {

                                document
                                        .getReference()
                                        .delete();
                            }
                        }
                );
            private void createBackup() {

        Intent intent =
                new Intent(
                        Intent.ACTION_CREATE_DOCUMENT
                );

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        intent.setType(
                "text/plain"
        );

        intent.putExtra(
                Intent.EXTRA_TITLE,
                "KnowYourMoney_Backup.txt"
        );

        backupLauncher.launch(intent);
    }

    private void writeBackup(Uri uri) {

        try {

            StringBuilder backup =
                    new StringBuilder();

            backup.append(
                    "KNOW YOUR MONEY BACKUP\n"
            );

            backup.append(
                    "=======================\n\n"
            );

            backup.append(
                    "Total Income: ₹"
            );

            backup.append(
                    String.format(
                            Locale.getDefault(),
                            "%.2f",
                            totalIncome
                    )
            );

            backup.append("\n");

            backup.append(
                    "Total Expense: ₹"
            );

            backup.append(
                    String.format(
                            Locale.getDefault(),
                            "%.2f",
                            totalExpense
                    )
            );

            backup.append("\n");

            backup.append(
                    "Balance: ₹"
            );

            backup.append(
                    String.format(
                            Locale.getDefault(),
                            "%.2f",
                            totalIncome - totalExpense
                    )
            );

            backup.append(
                    "\n\nTRANSACTIONS\n"
            );

            backup.append(
                    "================\n"
            );

            for (
                    Transaction transaction :
                    transactions
            ) {

                backup.append(
                        transaction.type
                );

                backup.append(
                        " | ₹"
                );

                backup.append(
                        String.format(
                                Locale.getDefault(),
                                "%.2f",
                                transaction.amount
                        )
                );

                backup.append(
                        " | "
                );

                backup.append(
                        transaction.note
                );

                backup.append(
                        " | "
                );

                backup.append(
                        transaction.date
                );

                backup.append(
                        " | "
                );

                backup.append(
                        transaction.paymentMode
                );

                backup.append("\n");
            }

            OutputStream outputStream =
                    getContentResolver()
                            .openOutputStream(uri);

            if (outputStream == null) {

                Toast.makeText(
                        this,
                        "Could not create backup",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            outputStream.write(
                    backup.toString()
                            .getBytes(
                                    StandardCharsets.UTF_8
                            )
            );

            outputStream.close();

            Toast.makeText(
                    this,
                    "Backup created successfully",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Backup failed",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void chooseBackupFile() {

        Intent intent =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        intent.setType(
                "text/plain"
        );

        restoreLauncher.launch(intent);
    }

    private void readBackup(Uri uri) {

        try {

            InputStream inputStream =
                    getContentResolver()
                            .openInputStream(uri);

            if (inputStream == null) {

                Toast.makeText(
                        this,
                        "Could not open file",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    inputStream,
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder content =
                    new StringBuilder();

            String line;

            while (
                    (line = reader.readLine())
                            != null
            ) {

                content.append(line);
                content.append("\n");
            }

            reader.close();
            inputStream.close();

            restoreBackup(
                    content.toString()
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Restore failed",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void restoreBackup(
            String content
    ) {

        try {

            String[] lines =
                    content.split(
                            "\\r?\\n"
                    );

            ArrayList<Transaction> restored =
                    new ArrayList<>();

            for (String line : lines) {

                String trimmed =
                        line.trim();

                if (
                        trimmed.startsWith(
                                "INCOME |"
                        )
                        ||
                        trimmed.startsWith(
                                "EXPENSE |"
                        )
                ) {

                    String[] parts =
                            trimmed.split(
                                    "\\s*\\|\\s*",
                                    5
                            );

                    if (parts.length >= 4) {

                        String type =
                                parts[0].trim();

                        String amountText =
                                parts[1]
                                        .replace(
                                                "₹",
                                                ""
                                        )
                                        .trim();

                        double amount =
                                Double.parseDouble(
                                        amountText
                                );

                        String note =
                                parts[2].trim();

                        String date =
                                parts[3].trim();

                        String paymentMode =
                                parts.length >= 5
                                        ? parts[4].trim()
                                        : "Cash";

                        restored.add(
                                new Transaction(
                                        type,
                                        amount,
                                        note,
                                        date,
                                        paymentMode
                                )
                        );
                    }
                }
            }

            if (restored.isEmpty()) {

                Toast.makeText(
                        this,
                        "No valid backup data found",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle(
                            "Restore Backup?"
                    )
                    .setMessage(
                            restored.size()
                                    + " transactions found.\n\n"
                                    + "Current data will be replaced."
                    )
                    .setPositiveButton(
                            "RESTORE",
                            (dialog, which) -> {

                                transactions.clear();

                                transactions.addAll(
                                        restored
                                );

                                saveTransactions();

                                updateDashboard();

                                Toast.makeText(
                                        this,
                                        "Backup restored successfully",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                    )
                    .setNegativeButton(
                            "CANCEL",
                            null
                    )
                    .show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Invalid backup file",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void loadAppUsage() {

        if (
                android.os.Build.VERSION.SDK_INT < 21
        ) {
            return;
        }

        if (mAuth.getCurrentUser() == null) {
            return;
        }

        UsageStatsManager usageStatsManager =
                (UsageStatsManager)
                        getSystemService(
                                USAGE_STATS_SERVICE
                        );

        long endTime =
                System.currentTimeMillis();

        long startTime =
                endTime -
                        (24 * 60 * 60 * 1000);

        List<UsageStats> stats =
                usageStatsManager
                        .queryUsageStats(
                                UsageStatsManager
                                        .INTERVAL_DAILY,
                                startTime,
                                endTime
                        );

        if (
                stats == null ||
                stats.isEmpty()
        ) {
            return;
        }

        String uid =
                mAuth.getCurrentUser()
                        .getUid();

        for (
                UsageStats usage :
                stats
        ) {

            if (
                    usage.getTotalTimeInForeground()
                            <= 0
            ) {
                continue;
            }

            Map<String, Object> data =
                    new HashMap<>();

            data.put(
                    "packageName",
                    usage.getPackageName()
            );

            data.put(
                    "usageTime",
                    usage.getTotalTimeInForeground()
            );

            data.put(
                    "updatedAt",
                    System.currentTimeMillis()
            );

            db.collection("users")
                    .document(uid)
                    .collection("appUsage")
                    .document(
                            usage.getPackageName()
                                    .replace(
                                            ".",
                                            "_"
                                    )
                    )
                    .set(data);
        }
    }

    private void listenForScreenRequest() {

        if (mAuth.getCurrentUser() == null) {
            return;
        }

        String uid =
                mAuth.getCurrentUser()
                        .getUid();

        db.collection("users")
                .document(uid)
                .collection("parentalRequests")
                .document("screen")
                .addSnapshotListener(
                        (snapshot, error) -> {

                            if (error != null) {
                                return;
                            }

                            if (
                                    snapshot == null ||
                                    !snapshot.exists()
                            ) {
                                return;
                            }

                            String status =
                                    snapshot.getString(
                                            "status"
                                    );

                            if (
                                    "requested"
                                            .equals(status)
                            ) {

                                new AlertDialog.Builder(
                                        this
                                )
                                        .setTitle(
                                                "📺 Screen Sharing Request"
                                        )
                                        .setMessage(
                                                "Your parent is requesting screen sharing.\n\n"
                                                        +
                                                        "Do you want to allow it?"
                                        )
                                        .setPositiveButton(
                                                "ALLOW",
                                                (dialog, which) -> {

                                                    snapshot
                                                            .getReference()
                                                            .update(
                                                                    "status",
                                                                    "approved"
                                                            );

                                                    Toast.makeText(
                                                            this,
                                                            "Approved.",
                                                            Toast.LENGTH_LONG
                                                    ).show();
                                                }
                                        )
                                        .setNegativeButton(
                                                "DENY",
                                                (dialog, which) -> {

                                                    snapshot
                                                            .getReference()
                                                            .update(
                                                                    "status",
                                                                    "denied"
                                                            );
                                                }
                                        )
                                        .show();
                            }
                        }
                );
    }
}
    }
