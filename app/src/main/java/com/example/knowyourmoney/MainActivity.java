package com.example.knowyourmoney;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView incomeTotalText;
    private TextView expenseTotalText;
    private TextView balanceText;
    private TextView historyText;

    private Button incomeButton;
    private Button expenseButton;
    private Button backupButton;
    private Button restoreButton;
    private Button clearHistoryButton;

    private SharedPreferences preferences;

    private final ArrayList<Transaction> transactions = new ArrayList<>();

    private double totalIncome = 0;
    private double totalExpense = 0;

    private ActivityResultLauncher<Intent> backupLauncher;
    private ActivityResultLauncher<Intent> restoreLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences("KnowYourMoney", MODE_PRIVATE);

        incomeTotalText = findViewById(R.id.incomeTotalText);
        expenseTotalText = findViewById(R.id.expenseTotalText);
        balanceText = findViewById(R.id.balanceText);
        historyText = findViewById(R.id.historyText);

        incomeButton = findViewById(R.id.incomeButton);
        expenseButton = findViewById(R.id.expenseButton);
        backupButton = findViewById(R.id.backupButton);
        restoreButton = findViewById(R.id.restoreButton);
        clearHistoryButton = findViewById(R.id.clearHistoryButton);

        setupBackupRestore();

        loadTransactions();
        updateDashboard();

        incomeButton.setOnClickListener(v -> showAddTransactionDialog(true));

        expenseButton.setOnClickListener(v -> showAddTransactionDialog(false));

        clearHistoryButton.setOnClickListener(v -> showClearHistoryDialog());

        backupButton.setOnClickListener(v -> backupData());

        restoreButton.setOnClickListener(v -> restoreData());

        historyText.setOnClickListener(v -> showDeleteTransactionDialog());
    }

    // ---------------------------------------------------------
    // TRANSACTION CLASS
    // ---------------------------------------------------------

    private static class Transaction {

        String type;
        double amount;
        String note;
        String date;

        Transaction(String type, double amount, String note, String date) {
            this.type = type;
            this.amount = amount;
            this.note = note;
            this.date = date;
        }
    }

    // ---------------------------------------------------------
    // ADD INCOME / EXPENSE
    // ---------------------------------------------------------

    private void showAddTransactionDialog(boolean isIncome) {

        final android.widget.EditText amountInput =
                new android.widget.EditText(this);

        amountInput.setHint("Enter amount");
        amountInput.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER
                        | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        final android.widget.EditText noteInput =
                new android.widget.EditText(this);

        noteInput.setHint("Note (optional)");

        android.widget.LinearLayout layout =
                new android.widget.LinearLayout(this);

        layout.setOrientation(android.widget.LinearLayout.VERTICAL);

        int padding = (int) (20 * getResources().getDisplayMetrics().density);

        layout.setPadding(padding, 10, padding, 0);

        layout.addView(amountInput);

        layout.addView(noteInput);

        String title;

        if (isIncome) {
            title = "Add Income";
        } else {
            title = "Add Expense";
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(layout)
                .setPositiveButton("SAVE", (dialog, which) -> {

                    String amountText =
                            amountInput.getText().toString().trim();

                    if (amountText.isEmpty()) {
                        Toast.makeText(
                                this,
                                "Please enter an amount",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    double amount;

                    try {
                        amount = Double.parseDouble(amountText);
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
                                "Amount must be greater than zero",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    String note =
                            noteInput.getText().toString().trim();

                    if (note.isEmpty()) {
                        if (isIncome) {
                            note = "Income";
                        } else {
                            note = "Expense";
                        }
                    }

                    String date =
                            new SimpleDateFormat(
                                    "dd MMM yyyy, hh:mm a",
                                    Locale.getDefault()
                            ).format(new Date());

                    String type;

                    if (isIncome) {
                        type = "INCOME";
                    } else {
                        type = "EXPENSE";
                    }

                    transactions.add(
                            new Transaction(
                                    type,
                                    amount,
                                    note,
                                    date
                            )
                    );

                    saveTransactions();

                    updateDashboard();

                    Toast.makeText(
                            this,
                            isIncome
                                    ? "Income added"
                                    : "Expense added",
                            Toast.LENGTH_SHORT
                    ).show();

                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    // ---------------------------------------------------------
    // UPDATE DASHBOARD
    // ---------------------------------------------------------

    private void updateDashboard() {

        totalIncome = 0;
        totalExpense = 0;

        for (Transaction transaction : transactions) {

            if (transaction.type.equals("INCOME")) {
                totalIncome += transaction.amount;
            }

            if (transaction.type.equals("EXPENSE")) {
                totalExpense += transaction.amount;
            }
        }

        double balance = totalIncome - totalExpense;

        incomeTotalText.setText(
                "₹" + String.format(
                        Locale.getDefault(),
                        "%.2f",
                        totalIncome
                )
        );

        expenseTotalText.setText(
                "₹" + String.format(
                        Locale.getDefault(),
                        "%.2f",
                        totalExpense
                )
        );

        balanceText.setText(
                "₹" + String.format(
                        Locale.getDefault(),
                        "%.2f",
                        balance
                )
        );

        updateHistory();
    }

    // ---------------------------------------------------------
    // HISTORY
    // ---------------------------------------------------------

    private void updateHistory() {

        if (transactions.isEmpty()) {

            historyText.setText(
                    "No transactions yet.\n\n" +
                            "Tap ADD INCOME or ADD EXPENSE to begin."
            );

            return;
        }

        StringBuilder history = new StringBuilder();

        for (int i = transactions.size() - 1; i >= 0; i--) {

            Transaction transaction = transactions.get(i);

            String symbol;

            if (transaction.type.equals("INCOME")) {
                symbol = "+";
            } else {
                symbol = "-";
            }

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
                    .append("\n\n");
        }

        historyText.setText(history.toString());
    }

    // ---------------------------------------------------------
    // DELETE TRANSACTION
    // ---------------------------------------------------------

    private void showDeleteTransactionDialog() {

        if (transactions.isEmpty()) {

            Toast.makeText(
                    this,
                    "No transactions to delete",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String[] items = new String[transactions.size()];

        for (int i = 0; i < transactions.size(); i++) {

            Transaction transaction = transactions.get(i);

            String symbol;

            if (transaction.type.equals("INCOME")) {
                symbol = "+";
            } else {
                symbol = "-";
            }

            items[i] =
                    symbol
                            + " ₹"
                            + String.format(
                            Locale.getDefault(),
                            "%.2f",
                            transaction.amount
                    )
                            + " - "
                            + transaction.note
                            + "\n"
                            + transaction.date;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Transaction")
                .setItems(items, (dialog, which) -> {

                    Transaction selected =
                            transactions.get(which);

                    new AlertDialog.Builder(this)
                            .setTitle("Delete this transaction?")
                            .setMessage(
                                    selected.type
                                            + "\n₹"
                                            + String.format(
                                            Locale.getDefault(),
                                            "%.2f",
                                            selected.amount
                                    )
                                            + "\n"
                                            + selected.note
                            )
                            .setPositiveButton(
                                    "DELETE",
                                    (d, w) -> {

                                        transactions.remove(which);

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
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    // ---------------------------------------------------------
    // CLEAR HISTORY
    // ---------------------------------------------------------

    private void showClearHistoryDialog() {

        if (transactions.isEmpty()) {

            Toast.makeText(
                    this,
                    "History is already empty",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Clear All History?")
                .setMessage(
                        "This will permanently remove all income " +
                                "and expense transactions from this app."
                )
                .setPositiveButton(
                        "CLEAR",
                        (dialog, which) -> {

                            transactions.clear();

                            saveTransactions();

                            updateDashboard();

                            Toast.makeText(
                                    this,
                                    "History cleared",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                )
                .setNegativeButton("CANCEL", null)
                .show();
    }

    // ---------------------------------------------------------
    // SAVE TRANSACTIONS
    // ---------------------------------------------------------

    private void saveTransactions() {

        JSONArray array = new JSONArray();

        try {

            for (Transaction transaction : transactions) {

                JSONObject object = new JSONObject();

                object.put("type", transaction.type);
                object.put("amount", transaction.amount);
                object.put("note", transaction.note);
                object.put("date", transaction.date);

                array.put(object);
            }

            preferences.edit()
                    .putString(
                            "transactions",
                            array.toString()
                    )
                    .apply();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not save data",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // ---------------------------------------------------------
    // LOAD TRANSACTIONS
    // ---------------------------------------------------------

    private void loadTransactions() {

        transactions.clear();

        String savedData =
                preferences.getString(
                        "transactions",
                        ""
                );

        if (savedData.isEmpty()) {
            return;
        }

        try {

            JSONArray array =
                    new JSONArray(savedData);

            for (int i = 0; i < array.length(); i++) {

                JSONObject object =
                        array.getJSONObject(i);

                String type =
                        object.getString("type");

                double amount =
                        object.getDouble("amount");

                String note =
                        object.getString("note");

                String date =
                        object.getString("date");

                transactions.add(
                        new Transaction(
                                type,
                                amount,
                                note,
                                date
                        )
                );
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not load saved data",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // ---------------------------------------------------------
    // BACKUP / RESTORE SETUP
    // ---------------------------------------------------------

    private void setupBackupRestore() {

        backupLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {

                            if (result.getResultCode()
                                    == RESULT_OK
                                    && result.getData() != null) {

                                Uri uri =
                                        result.getData().getData();

                                writeBackupFile(uri);
                            }
                        }
                );

        restoreLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {

                            if (result.getResultCode()
                                    == RESULT_OK
                                    && result.getData() != null) {

                                Uri uri =
                                        result.getData().getData();

                                readBackupFile(uri);
                            }
                        }
                );
    }

    // ---------------------------------------------------------
    // BACKUP DATA
    // ---------------------------------------------------------

    private void backupData() {

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

    private void writeBackupFile(Uri uri) {

        try {

            StringBuilder backup =
                    new StringBuilder();

            backup.append(
                    "KNOW YOUR MONEY BACKUP\n"
            );

            backup.append(
                    "======================\n\n"
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
                }
    }}
