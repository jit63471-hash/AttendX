package com.attendx.service;

import com.attendx.model.Course;
import com.attendx.model.Department;
import com.attendx.model.Semester;
import com.attendx.model.Subject;
import com.attendx.repository.CourseRepository;
import com.attendx.repository.DepartmentRepository;
import com.attendx.repository.SemesterRepository;
import com.attendx.repository.SubjectRepository;

import java.util.List;
import java.util.Optional;

public class DepartmentService {
    private final DepartmentRepository deptRepo = new DepartmentRepository();
    private final CourseRepository courseRepo = new CourseRepository();
    private final SemesterRepository semRepo = new SemesterRepository();
    private final SubjectRepository subjectRepo = new SubjectRepository();
    private final AuditService auditService = new AuditService();

    public List<Department> getAllDepartments() {
        return deptRepo.findAll();
    }

    public List<Department> getActiveDepartments() {
        return deptRepo.findAllActive();
    }

    public Optional<Department> getDepartmentById(int id) {
        return deptRepo.findById(id);
    }

    public boolean createDepartment(Department d) {
        boolean saved = deptRepo.save(d);
        if (saved) {
            auditService.log("CREATE_DEPARTMENT", "DEPARTMENT", d.getId(), null, d.getCode() + " - " + d.getName());
        }
        return saved;
    }

    public boolean updateDepartment(Department d) {
        boolean updated = deptRepo.update(d);
        if (updated) {
            auditService.log("UPDATE_DEPARTMENT", "DEPARTMENT", d.getId(), null, d.getCode() + " - " + d.getName());
        }
        return updated;
    }

    public List<Course> getCoursesByDepartment(int departmentId) {
        return courseRepo.findByDepartmentId(departmentId);
    }

    public List<Course> getAllCourses() {
        return courseRepo.findAll();
    }

    public List<Semester> getAllSemesters() {
        return semRepo.findAll();
    }

    public List<Subject> getSubjectsByCourseAndSemester(int courseId, int semesterId) {
        return subjectRepo.findByCourseAndSemester(courseId, semesterId);
    }

    public List<Subject> getAllSubjects() {
        return subjectRepo.findAll();
    }
}
