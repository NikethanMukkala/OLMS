package listeners;

import jobs.DueReminderJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class SchedulerListener implements ServletContextListener {

    private Scheduler scheduler;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();

            // Setup Due Reminder Job
            JobDetail job = JobBuilder.newJob(DueReminderJob.class)
                    .withIdentity("dueReminderJob", "group1")
                    .build();

            // Run every day at 08:00 AM
            // For testing, "0 0 8 * * ?" could be used. Let's use it.
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("dueReminderTrigger", "group1")
                    .withSchedule(CronScheduleBuilder.cronSchedule("0 0 8 * * ?")) 
                    .build();

            scheduler.scheduleJob(job, trigger);
            System.out.println("Scheduler started successfully.");

        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null) {
            try {
                scheduler.shutdown();
                System.out.println("Scheduler shutdown successfully.");
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
        }
    }
}
