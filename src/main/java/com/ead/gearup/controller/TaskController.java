package com.ead.gearup.controller;

import java.time.Instant;
import java.util.List;

import com.ead.gearup.dto.task.TaskUpdateDTO;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ead.gearup.dto.response.ApiResponseDTO;
import com.ead.gearup.dto.task.EmployeeRecentActivityDTO;
import com.ead.gearup.dto.task.TaskCreateDTO;
import com.ead.gearup.dto.task.TaskResponseDTO;
import com.ead.gearup.service.TaskService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tasks")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;

    // @RequiresRole({ UserRole.EMPLOYEE })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseDTO<TaskResponseDTO>> createTask(@RequestBody @Valid TaskCreateDTO taskCreateDTO,
            HttpServletRequest request) {

        TaskResponseDTO createdTask = taskService.createTask(taskCreateDTO);

        ApiResponseDTO<TaskResponseDTO> response = ApiResponseDTO.<TaskResponseDTO>builder()
                .status("success")
                .message("Task created successfully")
                .data(createdTask)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
   public ResponseEntity<ApiResponseDTO<TaskResponseDTO>> getTaskById(
           @PathVariable Long id, HttpServletRequest request) {

        TaskResponseDTO task = taskService.getTaskById(id);
        ApiResponseDTO<TaskResponseDTO> response = ApiResponseDTO.<TaskResponseDTO>builder()
                .status("success")
                .message("Task fetched successfully")
                .data(task)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);

   }

   @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
   public ResponseEntity<ApiResponseDTO<List<TaskResponseDTO>>> getAllTasks(HttpServletRequest request) {
        List<TaskResponseDTO> tasks = taskService.getAllTasks();

        ApiResponseDTO<List<TaskResponseDTO>> response = ApiResponseDTO.<List<TaskResponseDTO>>builder()
                .status("success")
               .message("Task fetched successfully")
               .data(tasks)
               .timestamp(Instant.now())
               .path(request.getRequestURI())
               .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
   }

   @PatchMapping(value = "/{id}",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
   public ResponseEntity<ApiResponseDTO<TaskResponseDTO>> updateTask(@PathVariable Long id,
           @RequestBody @Valid TaskUpdateDTO taskUpdateDTO,HttpServletRequest request){

        TaskResponseDTO updatedTask = taskService.updateTask(id,taskUpdateDTO);

        ApiResponseDTO<TaskResponseDTO> response = ApiResponseDTO.<TaskResponseDTO>builder()
                .status("success")
                .message("Task updated successfully")
                .data(updatedTask)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
   }

   @DeleteMapping("/{id}")
   public ResponseEntity<ApiResponseDTO<Void>> deleteTask(
           @PathVariable Long id, HttpServletRequest request) {

        taskService.deleteTask(id);

        ApiResponseDTO<Void> response = ApiResponseDTO.<Void>builder()
                .status("success")
                .message("Task deleted successfully")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(response);
   }

   @GetMapping("/employee/recent-activities")
   public ResponseEntity<ApiResponseDTO<List<EmployeeRecentActivityDTO>>> getRecentActivitiesForCurrentEmployee(
           HttpServletRequest request) {

        List<EmployeeRecentActivityDTO> recentActivities = taskService.getRecentActivitiesForCurrentEmployee();

        ApiResponseDTO<List<EmployeeRecentActivityDTO>> response = ApiResponseDTO.<List<EmployeeRecentActivityDTO>>builder()
                .status("success")
                .message("Recent activities fetched successfully")
                .data(recentActivities)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
   }



}
