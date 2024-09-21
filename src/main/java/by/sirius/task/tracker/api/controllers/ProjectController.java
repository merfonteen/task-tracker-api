package by.sirius.task.tracker.api.controllers;

import by.sirius.task.tracker.api.dto.AckDto;
import by.sirius.task.tracker.api.dto.ProjectDto;
import by.sirius.task.tracker.store.services.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@RestController
public class ProjectController {

    private final ProjectService projectService;

    public static final String FETCH_PROJECTS = "/api/projects";
    public static final String CREATE_PROJECT = "/api/projects";
    public static final String EDIT_PROJECT = "/api/projects/{project_id}";
    public static final String DELETE_PROJECT = "/api/projects/{project_id}";

    @GetMapping(FETCH_PROJECTS)
    public List<ProjectDto> fetchProjects(
            @RequestParam(value = "prefix_name", required = false) Optional<String> optionalPrefixName) {
       return projectService.fetchProjects(optionalPrefixName);
    }

    @PostMapping(CREATE_PROJECT)
    public ProjectDto createProject(@RequestParam String name) {
       return projectService.createProject(name);
    }

    @PatchMapping(EDIT_PROJECT)
    public ProjectDto editProject(@PathVariable("project_id") Long projectId, @RequestParam String name) {
        return projectService.editProject(projectId, name);
    }

    @DeleteMapping(DELETE_PROJECT)
    public AckDto deleteProject(@PathVariable("project_id") Long projectId) {
        return projectService.deleteProject(projectId);
    }
}
