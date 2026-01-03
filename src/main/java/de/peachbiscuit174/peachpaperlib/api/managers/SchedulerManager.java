package de.peachbiscuit174.peachpaperlib.api.managers;

import de.peachbiscuit174.peachpaperlib.PeachPaperLib;
import de.peachbiscuit174.peachpaperlib.scheduler.LibraryScheduler;

/**
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class SchedulerManager {

    private final LibraryScheduler scheduler = new LibraryScheduler(PeachPaperLib.getPlugin());

    public LibraryScheduler getScheduler() {
        return scheduler;
    }
}
