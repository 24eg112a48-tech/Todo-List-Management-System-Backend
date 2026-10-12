package com.example.todo.repository;

import com.example.todo.entity.Task;
import com.example.todo.enums.Priority;
import com.example.todo.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByStatus(TaskStatus status);

    List<Task> findByPriority(Priority priority);

    List<Task> findByStatusAndPriority(TaskStatus status, Priority priority);

    List<Task> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description);

    @Query("SELECT t FROM Task t WHERE t.dueDate < :today AND t.status <> 'COMPLETED'")
    List<Task> findOverdueTasks(LocalDate today);

    long countByStatus(TaskStatus status);
}
