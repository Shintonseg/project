package com.tible.ocm.services.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.tible.hawk.core.controllers.helpers.RequestHelper;
import com.tible.hawk.core.controllers.helpers.search.BaseTaskSearch;
import com.tible.hawk.core.controllers.helpers.search.DefaultPagination;
import com.tible.hawk.core.jobs.CommonTask;
import com.tible.hawk.core.models.*;
import com.tible.hawk.core.repositories.BaseTaskLeaderRepository;
import com.tible.hawk.core.repositories.BaseTaskParameterRepository;
import com.tible.hawk.core.repositories.BaseTaskRepository;
import com.tible.hawk.core.services.BaseEventLogService;
import com.tible.hawk.core.services.BaseNotificationService;
import com.tible.hawk.core.services.BaseRabbitInfoService;
import com.tible.hawk.core.services.impl.AbstractBaseTaskServiceImpl;
import com.tible.ocm.services.TaskService;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Primary
@Service
public class TaskServiceImpl extends AbstractBaseTaskServiceImpl<BaseTask, BaseTaskParameter, BaseNotification> implements TaskService {

   private static final QBaseTask QT = QBaseTask.baseTask;

    private final JPAQueryFactory queryFactory;

    protected TaskServiceImpl(BaseTaskRepository<BaseTask> taskRepository,
                              BaseTaskParameterRepository<BaseTaskParameter> taskParameterRepository,
                              BaseNotificationService<BaseNotification> notificationService,
                              BaseTaskLeaderRepository<BaseTaskLeader> taskLeaderRepository,
                              BaseEventLogService eventLogService,
                              BaseRabbitInfoService<BaseRabbitInfo> rabbitInfoService,
                              @Lazy List<? extends CommonTask<BaseTask, BaseTaskParameter>> commonTasks,
                              RedissonClient redissonClient,
                              JPAQueryFactory queryFactory) {
        super(taskRepository, taskParameterRepository, notificationService, taskLeaderRepository, eventLogService, rabbitInfoService, commonTasks, redissonClient);
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<BaseTask> list(DefaultPagination pagination) {

        BooleanBuilder bb = new BooleanBuilder();

        if (!StringUtils.isEmpty(pagination.getFilter())) {
            bb.and(QT.taskName.contains(pagination.getFilter()));
        }

        if (pagination instanceof BaseTaskSearch) {
            BaseTaskSearch taskSearch = (BaseTaskSearch) pagination;
            if (!StringUtils.isEmpty(taskSearch.getTaskType())) {
                bb.and(QT.taskName.eq(taskSearch.getTaskType()));
            }
            if (taskSearch.getDate() != null) {
                LocalDateTime from = taskSearch.getDate().truncatedTo(ChronoUnit.DAYS);
                bb.and(QT.start.between(from, from.plusDays(1)));
            }
        }

        return taskRepository.findAll(bb, RequestHelper.create(pagination));
    }

    @Override
    public List<BaseTask> listFiltered() {
        LocalDateTime now = LocalDateTime.now();
        BooleanBuilder bb = new BooleanBuilder();
        bb.and(QT.start.between(now.minusDays(14), now));
        return IterableUtils.toList(taskRepository.findAll(bb));
    }

    @Override
    protected BaseTask createTask() {
        return new BaseTask();
    }

    @Override
    protected BaseTaskParameter createTaskParameter() {
        return new BaseTaskParameter();
    }

    @Override
    public List<BaseTaskParameter> getTaskParameters(Long taskId) {
        QBaseTaskParameter qtp = QBaseTaskParameter.baseTaskParameter;
        return queryFactory.selectFrom(qtp).where(qtp.task.id.eq(taskId)).fetch();
    }
}
