package com.example.todov1app;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.todov1app.databinding.ActivityMainBinding;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements TaskRecyclerViewAdapter.DetailsButtonClickListener {

    ActivityMainBinding binding;
    ArrayList<Task> tasks = new ArrayList<>();
    TaskRecyclerViewAdapter taskRecyclerViewAdapter;
    int currentPosition = -1;
    ActionMode currentActionMode;

    private static final String PRIORITY_LOW = "Low";
    private static final String PRIORITY_MEDIUM = "Medium";
    private static final String PRIORITY_HIGH = "High";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Log.d("MainActivity", "onCreate");

        if(savedInstanceState != null) {
            tasks = (ArrayList<Task>) savedInstanceState.getSerializable("tasks");
            if (tasks == null) tasks = new ArrayList<>();
        }

        binding.addButton.setOnClickListener(view -> {
            if(currentActionMode != null)
                currentActionMode.finish();
            Intent i = new Intent(MainActivity.this, AddTaskActivity.class);
            addTaskResultLauncher.launch(i);
        });

        setupRecyclerView();

    }

    private void setupRecyclerView() {
        binding.taskListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.taskListRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        taskRecyclerViewAdapter = new TaskRecyclerViewAdapter(MainActivity.this, tasks);
        binding.taskListRecyclerView.setAdapter(taskRecyclerViewAdapter);

        taskRecyclerViewAdapter.setLongClickListener((view, position) -> {
            if (currentActionMode != null)
                return;

            currentPosition = position;
            currentActionMode = startActionMode(modeCallBack);
            view.setSelected(true);
        });

        taskRecyclerViewAdapter.setDetailsButtonClickListener(this);
    }

    @Override
    public void onDetailsButtonClick(View view, int position) {
        Log.d("MainActivity", "Details button clicked for position: " + position);
        if (currentActionMode != null) {
            currentActionMode.finish();
        }
        showTaskDetailsDialog(position);
    }

    ActivityResultLauncher<Intent> addTaskResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        Task tTask = (Task) data.getSerializableExtra("taskAdded");
                        if (tTask != null) {
                            Log.d("MainActivity", "Received Task: " + tTask.getName() + " Prio: " + tTask.getPriority());
                            tasks.add(tTask);
                            taskRecyclerViewAdapter.notifyItemInserted(tasks.size() - 1);
                        }
                    }
                }
            }
    );

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Log.d("MainActivity", "onSaveInstanceState()");
        savedInstanceState.putSerializable("tasks", tasks);
    }

    private void showTaskDetailsDialog(int position) {
        if (position < 0 || position >= tasks.size()) {
            Log.e("MainActivity", "Invalid position for details: " + position);
            Toast.makeText(this, "Cannot show details for this item.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        Task tTask = tasks.get(position);

        String priority = tTask.getPriority();
        if (priority == null || priority.trim().isEmpty()) {
            priority = "Not Set";
        }

        String tMsg = "Name: " + tTask.getName() + "\n\n" +
                "Description: " + (tTask.getDescription().isEmpty() ? "(No description)" : tTask.getDescription()) + "\n\n" +
                "Priority: " + priority;

        builder.setTitle("Task Details");
        builder.setMessage(tMsg);
        builder.setPositiveButton("OK", null);
        builder.create().show();
    }

    private ActionMode.Callback modeCallBack = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.setTitle("Actions");
            mode.getMenuInflater().inflate(R.menu.context_menu, menu);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (currentPosition < 0 || currentPosition >= tasks.size()) {
                Toast.makeText(MainActivity.this, "Error: Invalid item selected.", Toast.LENGTH_SHORT).show();
                mode.finish();
                return true;
            }

            int itemId = item.getItemId();
            Task tTask = tasks.get(currentPosition);

            if (itemId == R.id.deleteItem) {
                tasks.remove(currentPosition);
                taskRecyclerViewAdapter.notifyItemRemoved(currentPosition);
                mode.finish();
                return true;
            } else if (itemId == R.id.toTopItem) {
                Task removedTask = tasks.remove(currentPosition);
                tasks.add(0, removedTask);
                taskRecyclerViewAdapter.notifyItemMoved(currentPosition, 0);
                taskRecyclerViewAdapter.notifyItemChanged(0);
                if (tasks.size() > 1) taskRecyclerViewAdapter.notifyItemChanged(1);
                mode.finish();
                return true;
            } else if (itemId == R.id.toEndItem) {
                Task removedTask = tasks.remove(currentPosition);
                tasks.add(removedTask);
                int endPosition = tasks.size() - 1;
                taskRecyclerViewAdapter.notifyItemMoved(currentPosition, endPosition);
                taskRecyclerViewAdapter.notifyItemChanged(endPosition);
                mode.finish();
                return true;
            } else if (itemId == R.id.prioritizeItem) {
                String currentPriority = tTask.getPriority();
                String newPriority = null;

                if (PRIORITY_LOW.equalsIgnoreCase(currentPriority)) {
                    newPriority = PRIORITY_MEDIUM;
                } else if (PRIORITY_MEDIUM.equalsIgnoreCase(currentPriority)) {
                    newPriority = PRIORITY_HIGH;
                } else if (PRIORITY_HIGH.equalsIgnoreCase(currentPriority)) {
                    Toast.makeText(MainActivity.this, "Task already has High priority", Toast.LENGTH_SHORT).show();
                } else {
                    Log.w("MainActivity", "Task had unexpected priority '" + currentPriority + "', promoting to Medium.");
                    newPriority = PRIORITY_MEDIUM;
                }

                if (newPriority != null) {
                    tTask.setPriority(newPriority);
                    taskRecyclerViewAdapter.notifyItemChanged(currentPosition);
                    Log.d("MainActivity", "Task '" + tTask.getName() + "' priority changed to: " + newPriority);
                    Toast.makeText(MainActivity.this, "Priority set to " + newPriority, Toast.LENGTH_SHORT).show();
                }

                mode.finish();
                return true;

            } else {
                return false;
            }
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            currentActionMode = null;
            currentPosition = -1;
        }
    };
}