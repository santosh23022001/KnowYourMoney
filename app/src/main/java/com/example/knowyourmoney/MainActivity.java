package com.example.knowyourmoney;

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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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

    private final ArrayList<Transaction> transactions =
            new ArrayList<>();

    private double totalIncome = 0;
    private double totalExpense = 0;

    private ActivityResultLauncher<Intent> backupLauncher;
    private ActivityResultLauncher<Intent> restoreLauncher;

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

        updateDashboard();

        incomeButton.setOnClickListener(
                v -> showAddDialog(true)
        );

        expenseButton.setOnClickListener(
                v -> showAddDialog(false)
        );

        clearHistoryButton.setOnClickListener(
                v -> clearHistory()
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
            }

    private void showAddDialog(boolean income) {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        int padding =
                (int) (20 * getResources()
                        .getDisplayMetrics()
                        .density);

        layout.setPadding(
                padding,
                10,
                padding,
                0
        );

        EditText amountInput =
                new EditText(this);

        amountInput.setHint("Amount");

        amountInput.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER
                        | android.text.InputType
                        .TYPE_NUMBER_FLAG_DECIMAL
        );

        EditText noteInput =
                new EditText(this);

        noteInput.setHint("Note");

        layout.addView(amountInput);
        layout.addView(noteInput);

        String title;

        if (income) {
            title = "Add Income";
        } else {
            title = "Add Expense";
        }

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

                                if (income) {
                                    note = "Income";
                                } else {
                                    note = "Expense";
                                }
                            }

                            String type;

                            if (income) {
                                type = "INCOME";
                            } else {
                                type = "EXPENSE";
                            }

                            String date =
                                    new SimpleDateFormat(
                                            "dd MMM yyyy, hh:mm a",
                                            Locale.getDefault()
                                    ).format(
                                            new Date()
                                    );

                            transactions.add(
                                    new Transaction(
                                            type,
                                            amount,
                                            note,
                                            date,
"Cash"
                                    )
                            );

                            saveTransactions();

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

        for (Transaction transaction :
                transactions) {

            if (transaction.type.equals("INCOME")) {

                totalIncome +=
                        transaction.amount;

            } else if (
                    transaction.type.equals("EXPENSE")
            ) {

                totalExpense +=
                        transaction.amount;
            }
        }

        double balance =
                totalIncome - totalExpense;

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
                int i = transactions.size() - 1;
                i >= 0;
                i--
        ) {

            Transaction transaction =
                    transactions.get(i);

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

        historyText.setText(
        history.toString()
);
}

private void saveTransactions() {

        StringBuilder data =
                new StringBuilder();

        for (Transaction transaction :
                transactions) {

            data.append(transaction.type)
                    .append("|")
                    .append(transaction.amount)
                    .append("|")
                    .append(
                            transaction.note
                                    .replace("|", " ")
                    )
                    .append("|")
                    .append(transaction.date)
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
                    line.split("\\|", 4);

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

                transactions.add(
                        new Transaction(
                                type,
                                amount,
                                note,
                                date
                        )
                );

            } catch (Exception ignored) {
            }
        }
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
                new String[transactions.size()];

        for (
                int i = 0;
                i < transactions.size();
                i++
        ) {

            Transaction transaction =
                    transactions.get(i);

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
                            + transaction.note;
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

            for (Transaction transaction :
                    transactions) {

                backup.append(
                        transaction.type
                );

                backup.append(" | ₹");

                backup.append(
                        String.format(
                                Locale.getDefault(),
                                "%.2f",
                                transaction.amount
                        )
                );

                backup.append(" | ");

                backup.append(
                        transaction.note
                );

                backup.append(" | ");

                backup.append(
                        transaction.date
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
                    (line = reader.readLine()) != null
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
        private void restoreBackup(String content) {

        try {

            String[] lines =
                    content.split("\\r?\\n");

            ArrayList<Transaction> restored =
                    new ArrayList<>();

            for (String line : lines) {

                String trimmed =
                        line.trim();

                if (trimmed.startsWith("INCOME |")
                        || trimmed.startsWith("EXPENSE |")) {

                    String[] parts =
                            trimmed.split(
                                    "\\s*\\|\\s*",
                                    4
                            );

                    if (parts.length >= 4) {

                        String type =
                                parts[0].trim();

                        String amountText =
                                parts[1]
                                        .replace("₹", "")
                                        .trim();

                        double amount =
                                Double.parseDouble(
                                        amountText
                                );

                        String note =
                                parts[2].trim();

                        String date =
                                parts[3].trim();

                        restored.add(
                                new Transaction(
                                        type,
                                        amount,
                                        note,
                                        date
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
                    .setTitle("Restore Backup?")
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

}
