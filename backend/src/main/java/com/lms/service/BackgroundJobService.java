package com.lms.service;

import com.lms.model.BackgroundJob;
import com.lms.model.Payment;
import com.lms.repo.BackgroundJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Creates one database record per task and hands it to the asynchronous processor. */
@Service @RequiredArgsConstructor
public class BackgroundJobService {
    private final BackgroundJobRepository repository;
    private final BackgroundJobProcessor processor;
    private final JobNotificationService notifier;

    public void startPaymentJobs(Payment p) {
        for (String type : new String[]{"EMAIL", "INVOICE", "NOTIFICATION"}) {
            BackgroundJob job = new BackgroundJob();
            job.setPaymentId(p.getId());
            job.setJobType(type);
            job.setStatus("PENDING");
            job.setMessage("Waiting for processing");
            job.setStudentEmail(p.getStudent().getEmail());
            job.setCourseTitle(p.getCourse().getTitle());
            BackgroundJob saved = repository.save(job);
            notifier.publish(saved);
            processor.process(saved.getJobId());   // returns immediately; runs on a worker thread
        }
    }
}
