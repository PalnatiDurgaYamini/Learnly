package com.lms.service;

import com.lms.model.*;
import com.lms.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Central place for "can this user touch this course?" checks. */
@Component @RequiredArgsConstructor
public class Access {
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final CurrentUser me;

    public Course course(Long id) {
        return courses.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    }

    /** Course must be owned by the current instructor (or current user is admin). */
    public Course owned(Long id) {
        Course c = course(id);
        User u = me.get();
        if (u.getRole() != Role.ADMIN && !c.getInstructor().getId().equals(u.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your course");
        return c;
    }

    /** Owner, admin, or an enrolled student. */
    public Course member(Long id) {
        Course c = course(id);
        User u = me.get();
        boolean ok = u.getRole() == Role.ADMIN || c.getInstructor().getId().equals(u.getId())
                || enrollments.existsByStudentAndCourse(u, c);
        if (!ok) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Enroll in this course to access its content");
        return c;
    }
}
