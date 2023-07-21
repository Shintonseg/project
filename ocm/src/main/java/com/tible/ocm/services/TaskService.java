package com.tible.ocm.services;

import com.tible.hawk.core.models.BaseTask;
import com.tible.hawk.core.models.BaseTaskParameter;
import com.tible.hawk.core.services.BaseTaskService;

import java.util.List;

public interface TaskService extends BaseTaskService<BaseTask, BaseTaskParameter> {
    List<BaseTask> listFiltered();
}
