package com.example.knowyourmoney;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView balanceText;
    private TextView historyText;
    private TextView incomeTotalText;
    private TextView expenseTotalText;

    private Button incomeButton;
    private Button expenseButton;
    private Button backupButton;
    private Button restoreButton;

    private SharedPreferences preferences;

    private static final int CREATE_BACKUP_FILE = 100;
    private static final int OPEN_BACKUP_FILE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        balanceText = findViewById(R.id.balanceText);
        historyText = findViewById(R.id.historyText);
        incomeTotalText = findViewById(R.id.incomeTotalText);
        expenseTotalText = findViewById(R.id.expenseTotalText);

        incomeButton = findViewById(R.id.incomeButton);
        expenseButton = findViewById(R.id.expenseButton);
        backupButton = findViewById(R.id.backupButton);
        restoreButton = findViewById(R.id.restoreButton);

        preferences = getSharedPreferences("money_data", MODE_PRIVATE);

        incomeButton.setOnClickListener(v -> showAddIncomeDialog());
        expenseButton.setOnClickListener(v -> showAddExpenseDialog());

        backupButton.setOnClickListener(v -> createBackup());
        restoreButton.setOnClickListener(v -> chooseBackupFile());

        updateBalance();
        updateHistory();
    }

    private void updateBalance() {

        long incomeBits = preferences.getLong(
                "total_income",
                Double.doubleToLongBits(0)
        );

        long expenseBits = preferences.getLong(
                "total_expense",
                Double.doubleToLongBits(0)
        );

        double totalIncome = Double.longBitsToDouble(incomeBits);
        double totalExpense = Double.longBitsToDouble(expenseBits);

        double balance = totalIncome - totalExpense;

        incomeTotalText.setText(
                String.format(
                        Locale.getDefault(),
                        "Total Income: ₹%.2f",
                        totalIncome
                )
        );

        expenseTotalText.setText(
                String.format(
                        Locale.getDefault(),
                        "Total Expense: ₹%.2f",
                        totalExpense
                )
        );

        balanceText.setText(
                String.format(
                        Locale.getDefault(),
                        "Balance: ₹%.2f",
                        balance
                )
        );
    }

    private void updateHistory() {

        String history = preferences.getString("history", "");

        if (history.isEmpty()) {
            historyText.setText(
                    "Transaction History\n\nNo transactions yet."
            );
        } else {
            historyText.setText(
                    "Transaction History\n\n" + history
            );
        }
    }

    private void showAddIncomeDialog() {

        EditText amountInput = new EditText(this);
        amountInput.setHint("Amount");
        amountInput.setInputType(2);

        EditText sourceInput = new EditText(this);
        sourceInput.setHint("Income Source");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 10, 40, 10);

        layout.addView(amountInput);
        layout.addView(sourceInput);

        new AlertDialog.Builder(this)
                .setTitle("Add Income")
                .setView(layout)
                .setPositiveButton("ADD", (dialog, which) -> {

                    String amountText =
                            amountInput.getText().toString().trim();

                    String source =
                            sourceInput.getText().toString().trim();

                    if (amountText.isEmpty() || source.isEmpty()) {
                        Toast.makeText(
                                this,
                                "Enter amount and income source",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    try {

                        double amount =
                                Double.parseDouble(amountText);

                        if (amount <= 0) {
                            Toast.makeText(
                                    this,
                                    "Enter a valid amount",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        long incomeBits = preferences.getLong(
                                "total_income",
                                Double.doubleToLongBits(0)
                        );

                        double oldIncome =
                                Double.longBitsToDouble(incomeBits);

                        double newIncome =
                                oldIncome + amount;

                        String oldHistory =
                                preferences.getString("history", "");

                        String newEntry = String.format(
                                Locale.getDefault(),
                                "Income: ₹%.2f - %s",
                                amount,
                                source
                        );

                        String newHistory = newEntry;

                        if (!oldHistory.isEmpty()) {
                            newHistory =
                                    newEntry + "\n" + oldHistory;
                        }

                        preferences.edit()
                                .putLong(
                                        "total_income",
                                        Double.doubleToLongBits(newIncome)
                                )
                                .putString(
                                        "history",
                                        newHistory
                                )
                                .apply();

                        updateBalance();
                        updateHistory();

                        Toast.makeText(
                                this,
                                "Income added successfully!",
                                Toast.LENGTH_SHORT
                        ).show();

                    } catch (NumberFormatException e) {

                        Toast.makeText(
                                this,
                                "Enter a valid amount",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void showAddExpenseDialog() {

        EditText amountInput = new EditText(this);
        amountInput.setHint("Amount");
        amountInput.setInputType(2);

        EditText categoryInput = new EditText(this);
        categoryInput.setHint("Expense Category");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 10, 40, 10);

        layout.addView(amountInput);
        layout.addView(categoryInput);

        new AlertDialog.Builder(this)
                .setTitle("Add Expense")
                .setView(layout)
                .setPositiveButton("ADD", (dialog, which) -> {

                    String amountText =
                            amountInput.getText().toString().trim();

                    String category =
                            categoryInput.getText().toString().trim();

                    if (amountText.isEmpty() ||
                            category.isEmpty()) {

                        Toast.makeText(
                                this,
                                "Enter amount and expense category",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    try {

                        double amount =
                                Double.parseDouble(amountText);

                        if (amount <= 0) {
                            Toast.makeText(
                                    this,
                                    "Enter a valid amount",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        long expenseBits = preferences.getLong(
                                "total_expense",
                                Double.doubleToLongBits(0)
                        );

                        double oldExpense =
                                Double.longBitsToDouble(expenseBits);

                        double newExpense =
                                oldExpense + amount;

                        String oldHistory =
                                preferences.getString("history", "");

                        String newEntry = String.format(
                                Locale.getDefault(),
                                "Expense: ₹%.2f - %s",
                                amount,
                                category
                        );

                        String newHistory = newEntry;

                        if (!oldHistory.isEmpty()) {
                            newHistory =
                                    newEntry + "\n" + oldHistory;
                        }

                        preferences.edit()
                                .putLong(
                                        "total_expense",
                                        Double.doubleToLongBits(newExpense)
                                )
                                .putString(
                                        "history",
                                        newHistory
                                )
                                .apply();

                        updateBalance();
                        updateHistory();

                        Toast.makeText(
                                this,
                                "Expense added successfully!",
                                Toast.LENGTH_SHORT
                        ).show();

                    } catch (NumberFormatException e) {

                        Toast.makeText(
                                this,
                                "Enter a valid amount",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void createBackup() {

        String data =
                "KNOWYOURMONEY BACKUP\n" +
                "total_income=" +
                preferences.getLong(
                        "total_income",
                        Double.doubleToLongBits(0)
                ) +
                "\n" +
                "total_expense=" +
                preferences.getLong(
                        "total_expense",
                        Double.doubleToLongBits(0)
                ) +
                "\n" +
                "history=" +
                preferences.getString("history", "");

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("text/plain");
        intent.putExtra(
                Intent.EXTRA_TITLE,
                "KnowYourMoney_Backup.txt"
        );

        startActivityForResult(intent, CREATE_BACKUP_FILE);

        backupData = data;
    }

    private String backupData = "";

    private void saveBackup(Uri uri) {

        try {

            OutputStreamWriter writer =
                    new OutputStreamWriter(
                            getContentResolver().openOutputStream(uri)
                    );

            BufferedWriter bufferedWriter =
                    new BufferedWriter(writer);

            bufferedWriter.write(backupData);
            bufferedWriter.close();

            Toast.makeText(
                    this,
                    "Backup saved successfully!",
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

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("text/plain");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        startActivityForResult(intent, OPEN_BACKUP_FILE);
    }

    private void restoreBackup(Uri uri) {

        try {

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    getContentResolver()
                                            .openInputStream(uri)
                            )
                    );

            StringBuilder content = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            reader.close();

            String data = content.toString();

            if (!data.startsWith("KNOWYOURMONEY BACKUP")) {
                Toast.makeText(
                        this,
                        "Invalid backup file",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            String[] lines = data.split("\n");

            long incomeBits = 0;
            long expenseBits = 0;
            StringBuilder history = new StringBuilder();

            boolean historyStarted = false;

            for (String currentLine : lines) {

                if (currentLine.startsWith("total_income=")) {

                    incomeBits = Long.parseLong(
                            currentLine.substring(13).trim()
                    );

                } else if (currentLine.startsWith("total_expense=")) {

                    expenseBits = Long.parseLong(
                            currentLine.substring(14).trim()
                    );

                } else if (currentLine.startsWith("history=")) {

                    historyStarted = true;

                    history.append(
                            currentLine.substring(8)
                    );

                } else if (historyStarted) {

                    history.append("\n");
                    history.append(currentLine);
                }
            }

            new AlertDialog.Builder(this)
                    .setTitle("Restore Backup?")
                    .setMessage(
                            "This will replace your current money data with the backup."
                    )
                    .setNegativeButton("CANCEL", null)
                    .setPositiveButton("RESTORE", (dialog, which) -> {

                        preferences.edit()
                                .putLong(
                                        "total_income",
                                        incomeBits
                                )
                                .putLong(
                                        "total_expense",
                                        expenseBits
                                )
                                .putString(
                                        "history",
                                        history.toString()
                                )
                                .apply();

                        updateBalance();
                        updateHistory();

                        Toast.makeText(
                                this,
                                "Backup restored successfully!",
                                Toast.LENGTH_LONG
                        ).show();
                    })
                    .show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Restore failed",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (resultCode == RESULT_OK && data != null) {

            Uri uri = data.getData();

            if (requestCode == CREATE_BACKUP_FILE) {

                saveBackup(uri);

            } else if (requestCode == OPEN_BACKUP_FILE) {

                restoreBackup(uri);
            }
        }
    }
}
