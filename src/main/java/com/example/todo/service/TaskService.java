package com.example.todo.service;

import com.example.todo.dto.StatsResponse;
import com.example.todo.dto.TaskRequest;
import com.example.todo.entity.Task;
import com.example.todo.enums.Priority;
import com.example.todo.enums.TaskStatus;
import com.example.todo.exception.ResourceNotFoundException;
import com.example.todo.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    public Task create(TaskRequest request) {
        if (!StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("Title is required");
        }
        Task task = new Task();
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM);
        task.setStatus(request.getStatus() != null ? request.getStatus() : TaskStatus.PENDING);
        task.setDueDate(request.getDueDate());
        return repository.save(task);
    }

    @Transactional(readOnly = true)
    public List<Task> findAll(String search, TaskStatus status, Priority priority,
                              Boolean overdueOnly, String sortBy, String direction) {
        List<Task> tasks = repository.findAll();

        if (StringUtils.hasText(search)) {
            String q = search.trim().toLowerCase();
            tasks = tasks.stream()
                    .filter(t -> (t.getTitle() != null && t.getTitle().toLowerCase().contains(q))
                            || (t.getDescription() != null && t.getDescription().toLowerCase().contains(q)))
                    .toList();
        }
        if (status != null) {
            tasks = tasks.stream().filter(t -> t.getStatus() == status).toList();
        }
        if (priority != null) {
            tasks = tasks.stream().filter(t -> t.getPriority() == priority).toList();
        }
        if (Boolean.TRUE.equals(overdueOnly)) {
            LocalDate today = LocalDate.now();
            tasks = tasks.stream()
                    .filter(t -> t.getDueDate() != null
                            && t.getDueDate().isBefore(today)
                            && t.getStatus() != TaskStatus.COMPLETED)
                    .toList();
        }
        return sort(tasks, sortBy, direction);
    }

    private List<Task> sort(List<Task> tasks, String sortBy, String direction) {
        String key = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy.trim();
        boolean desc = "desc".equalsIgnoreCase(direction);
        Comparator<Task> comparator;
        switch (key) {
            case "dueDate" -> comparator = Comparator.comparing(Task::getDueDate,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case "priority" -> comparator = Comparator.comparing(Task::getPriority,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case "title" -> comparator = Comparator.comparing(Task::getTitle,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "status" -> comparator = Comparator.comparing(Task::getStatus,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case "updatedAt" -> comparator = Comparator.comparing(Task::getUpdatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            default -> comparator = Comparator.comparing(Task::getCreatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()));
        }
        if (desc) {
            comparator = comparator.reversed();
        }
        return tasks.stream().sorted(comparator).toList();
    }

    public Task findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));
    }

    public Task update(Long id, TaskRequest request) {
        Task task = findById(id);
        if (request.getTitle() != null) {
            if (!StringUtils.hasText(request.getTitle())) {
                throw new IllegalArgumentException("Title must not be blank");
            }
            task.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }
        return repository.save(task);
    }

    public void delete(Long id) {
        Task task = findById(id);
        repository.delete(task);
    }

    public Task markComplete(Long id) {
        Task task = findById(id);
        task.setStatus(TaskStatus.COMPLETED);
        return repository.save(task);
    }

    public Task markPending(Long id) {
        Task task = findById(id);
        task.setStatus(TaskStatus.PENDING);
        return repository.save(task);
    }

    @Transactional(readOnly = true)
    public StatsResponse stats() {
        StatsResponse s = new StatsResponse();
        long total = repository.count();
        long completed = repository.countByStatus(TaskStatus.COMPLETED);
        s.setTotal(total);
        s.setCompleted(completed);
        s.setPending(repository.countByStatus(TaskStatus.PENDING));
        s.setOverdue(repository.findOverdueTasks(LocalDate.now()).size());
        s.setCompletionPercentage(total == 0 ? 0.0
                : Math.round(completed * 1000.0 / total) / 10.0);
        return s;
    }
}
