package com.example.knowyourmoney;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView title = findViewById(R.id.titleText);
        Button incomeButton = findViewById(R.id.incomeButton);
        Button expenseButton = findViewById(R.id.expenseButton);

        incomeButton.setOnClickListener(v ->
                title.setText("Add Income"));

        expenseButton.setOnClickListener(v ->
                title.setText("Add Expense"));
    }
}
