package com.example.todo;

import com.example.todo.dto.TaskRequest;
import com.example.todo.entity.Task;
import com.example.todo.enums.Priority;
import com.example.todo.enums.TaskStatus;
import com.example.todo.exception.ResourceNotFoundException;
import com.example.todo.repository.TaskRepository;
import com.example.todo.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class TaskServiceTest {

    @Autowired
    private TaskService service;

    @Autowired
    private TaskRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    private TaskRequest req(String title, Priority p, TaskStatus s, LocalDate due) {
        TaskRequest r = new TaskRequest();
        r.setTitle(title);
        r.setDescription("desc for " + title);
        r.setPriority(p);
        r.setStatus(s);
        r.setDueDate(due);
        return r;
    }

    @Test
    void createAndFindTask() {
        Task created = service.create(req("Write tests", Priority.HIGH, TaskStatus.PENDING, LocalDate.now().plusDays(2)));
        assertThat(created.getId()).isNotNull();
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getUpdatedAt()).isNotNull();
        assertThat(service.findById(created.getId()).getTitle()).isEqualTo("Write tests");
    }

    @Test
    void createRequiresTitle() {
        TaskRequest r = new TaskRequest();
        r.setTitle("  ");
        assertThrows(IllegalArgumentException.class, () -> service.create(r));
    }

    @Test
    void updateRejectsBlankTitle() {
        Task t = service.create(req("Original", Priority.MEDIUM, TaskStatus.PENDING, null));
        TaskRequest r = new TaskRequest();
        r.setTitle("   ");
        assertThrows(IllegalArgumentException.class, () -> service.update(t.getId(), r));
    }

    @Test
    void findByIdThrowsForMissingId() {
        assertThrows(ResourceNotFoundException.class, () -> service.findById(999999L));
    }

    @Test
    void filterByStatusAndPriority() {
        service.create(req("A", Priority.HIGH, TaskStatus.PENDING, null));
        service.create(req("B", Priority.LOW, TaskStatus.COMPLETED, null));
        service.create(req("C", Priority.HIGH, TaskStatus.COMPLETED, null));

        assertThat(service.findAll(null, TaskStatus.PENDING, null, false, null, null)).hasSize(1);
        assertThat(service.findAll(null, null, Priority.HIGH, false, null, null)).hasSize(2);
        assertThat(service.findAll(null, TaskStatus.COMPLETED, Priority.HIGH, false, null, null)).hasSize(1);
    }

    @Test
    void searchFiltersByTitleOrDescription() {
        service.create(req("Buy milk", Priority.LOW, TaskStatus.PENDING, null));
        service.create(req("Read book", Priority.LOW, TaskStatus.PENDING, null));
        List<Task> found = service.findAll("milk", null, null, false, null, null);
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getTitle()).isEqualTo("Buy milk");
    }

    @Test
    void overdueFilterExcludesCompleted() {
        service.create(req("Old pending", Priority.MEDIUM, TaskStatus.PENDING, LocalDate.now().minusDays(3)));
        service.create(req("Old done", Priority.MEDIUM, TaskStatus.COMPLETED, LocalDate.now().minusDays(3)));
        service.create(req("Future", Priority.MEDIUM, TaskStatus.PENDING, LocalDate.now().plusDays(3)));
        List<Task> overdue = service.findAll(null, null, null, true, null, null);
        assertThat(overdue).hasSize(1);
        assertThat(overdue.get(0).getTitle()).isEqualTo("Old pending");
    }

    @Test
    void completeAndReopen() {
        Task t = service.create(req("Do it", Priority.MEDIUM, TaskStatus.PENDING, null));
        service.markComplete(t.getId());
        assertThat(service.findById(t.getId()).getStatus()).isEqualTo(TaskStatus.COMPLETED);
        service.markPending(t.getId());
        assertThat(service.findById(t.getId()).getStatus()).isEqualTo(TaskStatus.PENDING);
    }

    @Test
    void statsCountsAndCompletionPercentage() {
        service.create(req("One", Priority.HIGH, TaskStatus.PENDING, LocalDate.now().minusDays(1)));
        service.create(req("Two", Priority.LOW, TaskStatus.COMPLETED, null));
        var stats = service.stats();
        assertThat(stats.getTotal()).isEqualTo(2);
        assertThat(stats.getPending()).isEqualTo(1);
        assertThat(stats.getCompleted()).isEqualTo(1);
        assertThat(stats.getOverdue()).isEqualTo(1);
        assertThat(stats.getCompletionPercentage()).isEqualTo(50.0);
    }

    @Test
    void statsEmptyWhenNoTasks() {
        var stats = service.stats();
        assertThat(stats.getTotal()).isZero();
        assertThat(stats.getCompletionPercentage()).isZero();
    }
}
