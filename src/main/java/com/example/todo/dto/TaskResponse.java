package com.example.todo.dto;

import com.example.todo.entity.Task;
import com.example.todo.enums.Priority;
import com.example.todo.enums.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private Priority priority;
    private LocalDate dueDate;
    private TaskStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean overdue;

    public static TaskResponse from(Task task) {
        TaskResponse r = new TaskResponse();
        r.id = task.getId();
        r.title = task.getTitle();
        r.description = task.getDescription();
        r.priority = task.getPriority();
        r.dueDate = task.getDueDate();
        r.status = task.getStatus();
        r.createdAt = task.getCreatedAt();
        r.updatedAt = task.getUpdatedAt();
        r.overdue = task.getDueDate() != null
                && task.getStatus() != TaskStatus.COMPLETED
                && task.getDueDate().isBefore(LocalDate.now());
        return r;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Priority getPriority() {
        return priority;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isOverdue() {
        return overdue;
    }
}
