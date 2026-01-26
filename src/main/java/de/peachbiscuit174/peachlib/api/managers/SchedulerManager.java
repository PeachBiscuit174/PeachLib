package de.peachbiscuit174.peachlib.api.managers;

import de.peachbiscuit174.peachlib.PeachLib;
import de.peachbiscuit174.peachlib.scheduler.LibraryScheduler;

/**
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class SchedulerManager {

    private final LibraryScheduler scheduler = PeachLib.getScheduler();

    public LibraryScheduler getScheduler() {
        return scheduler;
    }
}
