package com.example.knowyourmoney;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {

    private TextView balanceText;
    private TextView historyText;
    private Button incomeButton;
    private Button expenseButton;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        balanceText = findViewById(R.id.balanceText);
        historyText = findViewById(R.id.historyText);
        incomeButton = findViewById(R.id.incomeButton);
        expenseButton = findViewById(R.id.expenseButton);

        preferences = getSharedPreferences("money_data", MODE_PRIVATE);

        incomeButton.setOnClickListener(v -> showAddIncomeDialog());

        expenseButton.setOnClickListener(v -> showAddExpenseDialog());

        updateBalance();
        updateHistory();
    }

    private void updateBalance() {

        long incomeBits = preferences.getLong("total_income", Double.doubleToLongBits(0));
        long expenseBits = preferences.getLong("total_expense", Double.doubleToLongBits(0));

        double totalIncome = Double.longBitsToDouble(incomeBits);
        double totalExpense = Double.longBitsToDouble(expenseBits);

        double balance = totalIncome - totalExpense;

        balanceText.setText(
                String.format(Locale.getDefault(), "Balance: ₹%.2f", balance)
        );
    }

    private void updateHistory() {

        String history = preferences.getString("history", "");

        if (history.isEmpty()) {
            historyText.setText("Transaction History\n\nNo transactions yet.");
        } else {
            historyText.setText("Transaction History\n\n" + history);
        }
    }

    private void showAddIncomeDialog() {

        EditText amountInput = new EditText(this);
        amountInput.setHint("Amount");

        EditText sourceInput = new EditText(this);
        sourceInput.setHint("Income Source");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 10, 40, 10);

        layout.addView(amountInput);
        layout.addView(sourceInput);

        new android.app.AlertDialog.Builder(this)
                .setTitle("Add Income")
                .setView(layout)
                .setPositiveButton("ADD", (dialog, which) -> {

                    String amountText = amountInput.getText().toString().trim();
                    String source = sourceInput.getText().toString().trim();

                    if (amountText.isEmpty() || source.isEmpty()) {
                        Toast.makeText(
                                this,
                                "Enter amount and income source",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    try {

                        double amount = Double.parseDouble(amountText);

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

                        double oldIncome = Double.longBitsToDouble(incomeBits);
                        double newIncome = oldIncome + amount;

                        String oldHistory = preferences.getString("history", "");

                        String newEntry = String.format(
                                Locale.getDefault(),
                                "Income: ₹%.2f - %s",
                                amount,
                                source
                        );

                        String newHistory = newEntry;

                        if (!oldHistory.isEmpty()) {
                            newHistory = newEntry + "\n" + oldHistory;
                        }

                        preferences.edit()
                                .putLong(
                                        "total_income",
                                        Double.doubleToLongBits(newIncome)
                                )
                                .putString("history", newHistory)
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

        EditText categoryInput = new EditText(this);
        categoryInput.setHint("Expense Category");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 10, 40, 10);

        layout.addView(amountInput);
        layout.addView(categoryInput);

        new android.app.AlertDialog.Builder(this)
                .setTitle("Add Expense")
                .setView(layout)
                .setPositiveButton("ADD", (dialog, which) -> {

                    String amountText = amountInput.getText().toString().trim();
                    String category = categoryInput.getText().toString().trim();

                    if (amountText.isEmpty() || category.isEmpty()) {
                        Toast.makeText(
                                this,
                                "Enter amount and expense category",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    try {

                        double amount = Double.parseDouble(amountText);

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

                        double oldExpense = Double.longBitsToDouble(expenseBits);
                        double newExpense = oldExpense + amount;

                        String oldHistory = preferences.getString("history", "");

                        String newEntry = String.format(
                                Locale.getDefault(),
                                "Expense: ₹%.2f - %s",
                                amount,
                                category
                        );

                        String newHistory = newEntry;

                        if (!oldHistory.isEmpty()) {
                            newHistory = newEntry + "\n" + oldHistory;
                        }

                        preferences.edit()
                                .putLong(
                                        "total_expense",
                                        Double.doubleToLongBits(newExpense)
                                )
                                .putString("history", newHistory)
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
}
