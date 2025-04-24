package com.example.todov1app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.example.todov1app.databinding.ActivityAddTaskBinding;

public class AddTaskActivity extends AppCompatActivity {

    ActivityAddTaskBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddTaskBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.addNewButton.setOnClickListener(view -> {
            String name = binding.taskNameEditText.getText().toString();
            String desc = binding.taskDescEditText.getText().toString();

            String priority = "";
            int selectedPriorityId = binding.priorityRadioGroup.getCheckedRadioButtonId();

            if (selectedPriorityId == -1) {
                Toast.makeText(this, "Please select a priority", Toast.LENGTH_SHORT).show();
                return;
            } else {
                RadioButton selectedRadioButton = findViewById(selectedPriorityId);
                priority = selectedRadioButton.getText().toString();
            }

            if (name.trim().isEmpty()) {
                Toast.makeText(this, "Task name cannot be empty", Toast.LENGTH_SHORT).show();
                binding.taskNameEditText.setError("Required");
                return;
            }

            Task task = new Task(name, desc, priority);
            Intent i = new Intent();
            i.putExtra("taskAdded", task);
            setResult(RESULT_OK, i);
            AddTaskActivity.this.finish();
        });

        binding.radioMedium.setChecked(true);
    }
}