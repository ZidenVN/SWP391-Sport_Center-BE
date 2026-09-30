package com.fptu.swp391.sportscentermanager.service;

import com.fptu.swp391.sportscentermanager.entity.Subject;

import java.util.List;

public interface SubjectService {
    List<Subject> getAllSubjects();
    Subject getSubjectById(Long id);
    Subject createSubject(Subject subject);
    Subject updateSubject(Long id, Subject subjectDetails);
    void deleteSubjectById(Long id);
}
