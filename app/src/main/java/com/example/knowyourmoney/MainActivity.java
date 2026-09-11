package com.example.knowyourmoney;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.util.ArrayList;
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
    private Button clearHistoryButton;

    private SharedPreferences preferences;

    private static final int CREATE_BACKUP_FILE = 100;
    private static final int OPEN_BACKUP_FILE = 101;

    private String backupData = "";

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
        clearHistoryButton = findViewById(R.id.clearHistoryButton);

        preferences = getSharedPreferences("money_data", MODE_PRIVATE);

        incomeButton.setOnClickListener(v -> showAddIncomeDialog());
        expenseButton.setOnClickListener(v -> showAddExpenseDialog());
        backupButton.setOnClickListener(v -> createBackup());
        restoreButton.setOnClickListener(v -> chooseBackupFile());
        clearHistoryButton.setOnClickListener(v -> showClearHistoryDialog());

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

        incomeTotalText.setText(String.format(
                Locale.getDefault(),
                "Total Income: ₹%.2f",
                totalIncome
        ));

        expenseTotalText.setText(String.format(
                Locale.getDefault(),
                "Total Expense: ₹%.2f",
                totalExpense
        ));

        balanceText.setText(String.format(
                Locale.getDefault(),
                "Balance: ₹%.2f",
                balance
        ));
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

            historyText.setOnClickListener(v -> showDeleteTransactionDialog());
        }
    }

    private void showAddIncomeDialog() {

        EditText amountInput = new EditText(this);
        amountInput.setHint("Amount");
        amountInput.setInputType(InputType.TYPE_CLASS_NUMBER |
                InputType.TYPE_NUMBER_FLAG_DECIMAL);

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

                    if (amountText.isEmpty() ||
                            source.isEmpty()) {

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

                        long incomeBits =
                                preferences.getLong(
                                        "total_income",
                                        Double.doubleToLongBits(0)
                                );

                        double oldIncome =
                                Double.longBitsToDouble(incomeBits);

                        double newIncome =
                                oldIncome + amount;

                        String oldHistory =
                                preferences.getString(
                                        "history",
                                        ""
                                );

                        String newEntry =
                                String.format(
                                        Locale.getDefault(),
                                        "Income: ₹%.2f - %s",
                                        amount,
                                        source
                                );

                        String newHistory =
                                newEntry;

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
        amountInput.setInputType(InputType.TYPE_CLASS_NUMBER |
                InputType.TYPE_NUMBER_FLAG_DECIMAL);

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

                        long expenseBits =
                                preferences.getLong(
                                        "total_expense",
                                        Double.doubleToLongBits(0)
                                );

                        double oldExpense =
                                Double.longBitsToDouble(expenseBits);

                        double newExpense =
                                oldExpense + amount;

                        String oldHistory =
                                preferences.getString(
                                        "history",
                                        ""
                                );

                        String newEntry =
                                String.format(
                                        Locale.getDefault(),
                                        "Expense: ₹%.2f - %s",
                                        amount,
                                        category
                                );

                        String newHistory =
                                newEntry;

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

    private void showDeleteTransactionDialog() {

        String history =
                preferences.getString("history", "");

        if (history.isEmpty()) {

            Toast.makeText(
                    this,
                    "No transactions to delete",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String[] transactions =
                history.split("\n");

        ArrayList<String> transactionList =
                new ArrayList<>();

        for (String transaction : transactions) {

            if (!transaction.trim().isEmpty()) {

                transactionList.add(transaction);
            }
        }

        String[] items =
                transactionList.toArray(
                        new String[0]
                );

        new AlertDialog.Builder(this)
                .setTitle("Select Transaction to Delete")

                .setItems(items, (dialog, which) -> {

                    String selected =
                            transactionList.get(which);

                    confirmDeleteTransaction(
                            selected,
                            history
                    );
                })

                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void confirmDeleteTransaction(
            String selected,
            String history) {

        new AlertDialog.Builder(this)
                .setTitle("Delete Transaction?")
                .setMessage(
                        selected +
                                "\n\nThis transaction will be deleted."
                )

                .setNegativeButton("CANCEL", null)

                .setPositiveButton(
                        "DELETE",
                        (dialog, which) -> {

                            deleteTransaction(
                                    selected,
                                    history
                            );
                        }
                )
                .show();
    }

    private void deleteTransaction(
            String selected,
            String history) {

        String[] transactions =
                history.split("\n");

        StringBuilder newHistory =
                new StringBuilder();

        boolean deleted = false;

        double incomeToRemove = 0;
        double expenseToRemove = 0;

        for (String transaction : transactions) {

            if (!deleted &&
                    transaction.equals(selected)) {

                deleted = true;

                double amount =
                        getTransactionAmount(transaction);

                if (transaction.startsWith("Income:")) {

                    incomeToRemove = amount;

                } else if (transaction.startsWith("Expense:")) {

                    expenseToRemove = amount;
                }

                continue;
            }

            if (!transaction.trim().isEmpty()) {

                if (newHistory.length() > 0) {

                    newHistory.append("\n");
                }

                newHistory.append(transaction);
            }
        }

        long incomeBits =
                preferences.getLong(
                        "total_income",
                        Double.doubleToLongBits(0)
                );

        long expenseBits =
                preferences.getLong(
                        "total_expense",
                        Double.doubleToLongBits(0)
                );

        double oldIncome =
                Double.longBitsToDouble(incomeBits);

        double oldExpense =
                Double.longBitsToDouble(expenseBits);

        double newIncome =
                Math.max(0, oldIncome - incomeToRemove);

        double newExpense =
                Math.max(0, oldExpense - expenseToRemove);

        preferences.edit()
                .putLong(
                        "total_income",
                        Double.doubleToLongBits(newIncome)
                )
                .putLong(
                        "total_expense",
                        Double.doubleToLongBits(newExpense)
                )
                .putString(
                        "history",
                        newHistory.toString()
                )
                .apply();

        updateBalance();
        updateHistory();

        Toast.makeText(
                this,
                "Transaction deleted!",
                Toast.LENGTH_SHORT
        ).show();
    }

    private double getTransactionAmount(
            String transaction) {

        try {

            int rupeePosition =
                    transaction.indexOf("₹");

            int dashPosition =
                    transaction.indexOf(" - ");

            if (rupeePosition >= 0 &&
                    dashPosition > rupeePosition) {

                String amountText =
                        transaction.substring(
                                rupeePosition + 1,
                                dashPosition
                        ).trim();

                return Double.parseDouble(amountText);
            }

        } catch (Exception e) {

            return 0;
        }

        return 0;
    }

    private void showClearHistoryDialog() {

        new AlertDialog.Builder(this)
                .setTitle("Clear All History?")

                .setMessage(
                        "This will delete all transactions and reset income, expense and balance to ₹0.00."
                )

                .setNegativeButton(
                        "CANCEL",
                        null
                )

                .setPositiveButton(
                        "CLEAR",
                        (dialog, which) -> {

                            preferences.edit()
                                    .putLong(
                                            "total_income",
                                            Double.doubleToLongBits(0)
                                    )
                                    .putLong(
                                            "total_expense",
                                            Double.doubleToLongBits(0)
                                    )
                                    .putString(
                                            "history",
                                            ""
                                    )
                                    .apply();

                            updateBalance();
                            updateHistory();

                            Toast.makeText(
                                    this,
                                    "All history cleared!",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                )
                .show();
    }

    private void createBackup() {

        backupData =
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
                preferences.getString(
                        "history",
                        ""
                );

       
