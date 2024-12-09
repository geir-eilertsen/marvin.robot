package com.assetvisor.marvin.equipment.notebook;

import com.assetvisor.marvin.robot.application.ListenUseCase;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NoteBook {
    @Resource
    private ForPersistingNotes forPersistingNotes;
    @Resource
    private ListenUseCase listenUseCase;

    public void takeNote(CalendarNote note) {
        forPersistingNotes.persist(note);
    }

    public void readNoteForNow() {
        forPersistingNotes.all().stream()
            .filter(note -> note.noteDate().isBefore(LocalDateTime.now()))
            .forEach(note -> {
                listenUseCase.listenTo(note.note());
                forPersistingNotes.delete(note);
            });
    }

    @Scheduled(fixedDelay = 60000)
    public void processNoteBook() {
        readNoteForNow();
    }
}
