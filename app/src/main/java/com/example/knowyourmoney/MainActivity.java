package com.example.knowyourmoney;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.view.ViewGroup;
import android.view.Gravity;

public class MainActivity extends Activity {

    private TextView balanceText;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        balanceText = findViewById(R.id.balanceText);

        Button incomeButton = findViewById(R.id.incomeButton);
        Button expenseButton = findViewById(R.id.expenseButton);

        preferences = getSharedPreferences("money_data", MODE_PRIVATE);

        updateBalance();

        incomeButton.setOnClickListener(v -> showAddIncomeDialog());

        expenseButton.setOnClickListener(v -> showAddExpenseDialog());
    }

    private void showAddIncomeDialog() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 10, 40, 10);

        EditText amountInput = new EditText(this);
        amountInput.setHint("Amount");
        amountInput.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER |
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        EditText sourceInput = new EditText(this);
        sourceInput.setHint("Income Source");

        layout.addView(amountInput,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

        layout.addView(sourceInput,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add Income")
                .setView(layout)
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("SAVE", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

                String amountText = amountInput.getText().toString().trim();
                String source = sourceInput.getText().toString().trim();

                if (amountText.isEmpty()) {
                    amountInput.setError("Enter amount");
                    return;
                }

                if (source.isEmpty()) {
                    sourceInput.setError("Enter income source");
                    return;
                }

                double amount = Double.parseDouble(amountText);

                double oldIncome =
                        Double.longBitsToDouble(
                                preferences.getLong(
                                        "total_income",
                                        Double.doubleToLongBits(0.0)
                                )
                        );

                double newIncome = oldIncome + amount;

                preferences.edit()
                        .putLong(
                                "total_income",
                                Double.doubleToLongBits(newIncome)
                        )
                        .apply();

                updateBalance();

                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void updateBalance() {

        double income =
                Double.longBitsToDouble(
                        preferences.getLong(
                                "total_income",
                                Double.doubleToLongBits(0.0)
                        )
                );

        double expense =
                Double.longBitsToDouble(
                        preferences.getLong(
                                "total_expense",
                                Double.doubleToLongBits(0.0)
                        )
                );

        double balance = income - expense;

        balanceText.setText(
                String.format("Balance: ₹%.2f", balance)
        );
    }private void showAddExpenseDialog() {

    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(40, 10, 40, 10);

    EditText amountInput = new EditText(this);
    amountInput.setHint("Amount");
    amountInput.setInputType(
            android.text.InputType.TYPE_CLASS_NUMBER |
            android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
    );

    EditText categoryInput = new EditText(this);
    categoryInput.setHint("Expense Category");

    layout.addView(amountInput,
            new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

    layout.addView(categoryInput,
            new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

    AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Add Expense")
            .setView(layout)
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("SAVE", null)
            .create();

    dialog.setOnShowListener(d -> {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

            String amountText = amountInput.getText().toString().trim();
            String category = categoryInput.getText().toString().trim();

            if (amountText.isEmpty()) {
                amountInput.setError("Enter amount");
                return;
            }

            if (category.isEmpty()) {
                categoryInput.setError("Enter category");
                return;
            }

            double amount = Double.parseDouble(amountText);

            double oldExpense =
                    Double.longBitsToDouble(
                            preferences.getLong(
                                    "total_expense",
                                    Double.doubleToLongBits(0.0)
                            )
                    );

            double newExpense = oldExpense + amount;

            preferences.edit()
                    .putLong(
                            "total_expense",
                            Double.doubleToLongBits(newExpense)
                    )
                    .apply();

            updateBalance();

            dialog.dismiss();
        });
    });

    dialog.show();
    }
}
