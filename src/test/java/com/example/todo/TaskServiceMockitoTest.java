package com.example.todo;

import com.example.todo.dto.TaskRequest;
import com.example.todo.entity.Task;
import com.example.todo.enums.Priority;
import com.example.todo.enums.TaskStatus;
import com.example.todo.exception.ResourceNotFoundException;
import com.example.todo.repository.TaskRepository;
import com.example.todo.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceMockitoTest {

    @Mock
    private TaskRepository repository;

    @InjectMocks
    private TaskService service;

    @Test
    void createAppliesDefaultsAndSaves() {
        TaskRequest r = new TaskRequest();
        r.setTitle("  Mocked task  ");
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task saved = service.create(r);

        assertThat(saved.getTitle()).isEqualTo("Mocked task");
        assertThat(saved.getPriority()).isEqualTo(Priority.MEDIUM);
        assertThat(saved.getStatus()).isEqualTo(TaskStatus.PENDING);
        verify(repository).save(any(Task.class));
    }

    @Test
    void findByIdThrowsWhenRepositoryEmpty() {
        when(repository.findById(42L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.findById(42L));
    }

    @Test
    void markCompleteSetsCompletedStatus() {
        Task task = new Task();
        task.setId(7L);
        task.setTitle("T");
        task.setStatus(TaskStatus.PENDING);
        when(repository.findById(7L)).thenReturn(Optional.of(task));
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = service.markComplete(7L);

        assertThat(result.getStatus()).isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    void statsComputesCompletionPercentageFromRepository() {
        when(repository.count()).thenReturn(4L);
        when(repository.countByStatus(TaskStatus.COMPLETED)).thenReturn(1L);
        when(repository.countByStatus(TaskStatus.PENDING)).thenReturn(3L);
        when(repository.findOverdueTasks(LocalDate.now())).thenReturn(List.of(new Task()));

        var stats = service.stats();

        assertThat(stats.getTotal()).isEqualTo(4);
        assertThat(stats.getCompleted()).isEqualTo(1);
        assertThat(stats.getPending()).isEqualTo(3);
        assertThat(stats.getOverdue()).isEqualTo(1);
        assertThat(stats.getCompletionPercentage()).isEqualTo(25.0);
    }
}
