package com.assetvisor.marvin.robot.application.services;

import com.assetvisor.marvin.equipment.notebook.NoteBook;
import com.assetvisor.marvin.robot.application.ListenUseCase;
import com.assetvisor.marvin.robot.domain.brain.ForInvokingBrain;
import com.assetvisor.marvin.robot.domain.environment.ForGettingEnvironmentFunctions;
import com.assetvisor.marvin.robot.domain.environment.Functions;
import com.assetvisor.marvin.robot.domain.jobdescription.ForPersistingRobotDescription;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class LifecycleService {
    @Resource
    private ForInvokingBrain forInvokingBrain;
    @Resource
    private ForPersistingRobotDescription forPersistingRobotDescription;
    @Resource
    private NoteBook noteBook;
    @Resource
    private ForGettingEnvironmentFunctions forGettingEnvironmentFunctions;
    @Resource
    private ListenUseCase listenUseCase;

    @PostConstruct
    public void wakeUp() {
        Functions functions = new Functions(
            forGettingEnvironmentFunctions,
            noteBook
        );
        forInvokingBrain.wakeUp(
            forPersistingRobotDescription.read(),
            functions.all()
        );
        listenUseCase.listenTo("You were just woken up, check the current time.");
    }
}
