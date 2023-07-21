package com.tible.ocm.controllers;

import com.tible.hawk.core.controllers.AbstractTaskController;
import com.tible.hawk.core.controllers.details.BaseTaskDetail;
import com.tible.hawk.core.controllers.details.BaseTaskParameterDetail;
import com.tible.hawk.core.controllers.helpers.ReflectUtils;
import com.tible.hawk.core.models.BaseTask;
import com.tible.hawk.core.models.BaseTaskParameter;
import com.tible.ocm.models.OcmTaskType;
import com.tible.ocm.services.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/task")
@PreAuthorize("#oauth2.hasScope('tible')")
public class TaskController extends AbstractTaskController<BaseTask, BaseTaskDetail, BaseTaskParameter> {

    private final TaskService taskService;
    private final ConversionService conversionService;

    @Autowired
    public TaskController(TaskService taskService,
                          ConversionService conversionService) {
        super(taskService);
        this.taskService = taskService;
        this.conversionService = conversionService;
    }

    @GetMapping("/runTask")
    public void runTask(@RequestParam String taskType) {
        super.run(taskType, null);
    }

    @GetMapping("/list")
    public List<BaseTaskDetail> list() {
        return taskService.listFiltered().stream().map(this::convertFrom).collect(Collectors.toList());
    }

    @GetMapping("/isStarted/{name}")
    @Override
    public boolean isStarted(@PathVariable String name) {
        return super.isStarted(name);
    }

    @GetMapping("/stop")
    @Override
    public void stop(@RequestParam Long id) {
        super.stop(id);
    }

    @GetMapping("/remove")
    @Override
    public void remove(@RequestParam Long id) {
        super.remove(id);
    }

    @GetMapping("/taskType")
    public List<String> taskType() {
        List<String> taskTypes = ReflectUtils.getStaticFields(OcmTaskType.class);
        Collections.sort(taskTypes);
        return taskTypes;
    }

    @Override
    protected BaseTaskDetail convertFrom(BaseTask task) {
        return BaseTaskDetail.from(task);
    }

    @Override
    protected BaseTaskParameter convertTo(BaseTaskParameterDetail taskParameterDetail) {
        return conversionService.convert(taskParameterDetail, BaseTaskParameter.class);
    }
}
