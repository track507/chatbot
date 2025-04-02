

-- View to show all students with their enrolled sections
-- drop view if exists StudentEnrollments;
-- create view StudentEnrollments as
-- select s.id as StudentID, s.firstname || ' ' || s.lastname as StudentName, sec.crn as SectionID, sec.courseID as Course, sec.term, sec.days, sec.room 
-- from student s 
-- join student_section ss on s.id = ss.studentID
-- join section sec on ss.sectionID = sec.crn;

-- -- View to show prerequisites for courses
-- drop view if exists CoursePrerequisites;
-- create view CoursePrerequisites as
-- select c.id as CourseID, c.title as CourseTitle, p.prereq as PrerequisiteCourse 
-- from course c 
-- join prereq p on c.id = p.course;

-- -- View to show students with their majors
-- drop view if exists StudentMajors;
-- create view StudentMajors as
-- select s.id as StudentID, s.firstname || ' ' || s.lastname as StudentName, sm.major as Major 
-- from student s 
-- join student_major sm on s.id = sm.studentID;

-- -- View to show courses in a major
-- drop view if exists MajorCourses;
-- create view MajorCourses as
-- select m.id as MajorID, m.title as MajorName, c.id as CourseID, c.title as CourseTitle 
-- from major m 
-- join major_class mc on m.id = mc.majorID 
-- join course c on mc.classID = c.id
-- ORDER BY m.id;

-- view to compute remaining credit hours using existing tables
drop view if exists remaining_hours1;
create view remaining_hours1 as
select s.id as studentID, sm.major as MajorID, 
       m.hrs as total_required,
       COALESCE(SUM(DISTINCT c.hrs), 0) as completed_hours,
       m.hrs - COALESCE(SUM(DISTINCT c.hrs), 0) as remaining_hours
from student s
join student_major sm on s.id = sm.studentID
join major m on sm.major = m.id
LEFT join student_section ss on s.id = ss.studentID
LEFT join section sec on ss.sectionID = sec.crn
LEFT join course c on sec.courseID = c.id
LEFT join major_class mc on c.id = mc.classID AND mc.majorID = sm.major
group by s.id, sm.major;

drop view if exists remaining_hours2;
create view remaining_hours2 as
select 
    s.id as studentID,
    m.hrs as total_required,

    -- Only sum hours of courses part of this student's major
    COALESCE((
        select SUM(DISTINCT c.hrs)
        from student_section ss
        join section sc on ss.sectionID = sc.crn
        join course c on sc.courseID = c.id
        join major_class mc on c.id = mc.classID
        where ss.studentID = s.id
          AND mc.majorID = sm.major
    ), 0) as completed_hours,

    -- Remaining = total - completed
    m.hrs - COALESCE((
        select SUM(DISTINCT c.hrs)
        from student_section ss
        join section sc on ss.sectionID = sc.crn
        join course c on sc.courseID = c.id
        join major_class mc on c.id = mc.classID
        where ss.studentID = s.id
          AND mc.majorID = sm.major
    ), 0) as remaining_hours

from student s
join student_major sm on s.id = sm.studentID
join major m on sm.major = m.id;

drop view if exists sections_students;
create view sections_students as select ss.studentID, s.crn, s.courseID, s.term, s.days, s.max from section s inner join student_section ss on s.crn = ss.sectionID ORDER BY ss.studentID;
drop view if exists v_and_prereq;
drop view if exists v_or_prereq;
drop view if exists v_coreq;
create view v_and_prereq as select ap.course, ap.prereq from and_prereq as ap inner join course as c on ap.course = c.id;
create view v_or_prereq as select op.course, op.prereq from or_prereq as op inner join course as c on op.course = c.id;
create view v_coreq as select co.course, co.prereq from coreq as co inner join course as c on co.course = c.id;

-- Stuff for views for class
drop view if exists MajorConcentrations;
create view MajorConcentrations as
select
    m.id as MajorID,
    m.title as MajorName,
    c.id as ConcentrationID,
    c.title as ConcentrationName,
    c.reqtext as Requirements
from major m
join concentration c on m.id = c.major;

drop view if exists StudentClasses;
create view StudentClasses as
select s.id as StudentID, s.firstname || ' ' || s.lastname as StudentName, 
       sec.courseID as CourseID, c.title as CourseTitle, sec.term
from student s
join student_section ss on s.id = ss.studentID
join section sec on ss.sectionID = sec.crn
join course c on sec.courseID = c.id;

drop view if exists StudentCreditHours;
create view StudentCreditHours as
select s.id as StudentID, SUM(c.hrs) as TotalCreditHours
from student s
join student_section ss on s.id = ss.studentID
join section sec on ss.sectionID = sec.crn
join course c on sec.courseID = c.id
group by s.id;

drop view if exists StudentDepartment;
create view StudentDepartment as
select s.id as StudentID, d.id as DepartmentID, d.name as DepartmentName
from student s
join student_major sm on s.id = sm.studentID
join major m on sm.major = m.id
join department d on m.deptID = d.id;

drop view if exists CourseProfessor;
create view CourseProfessor as
select sec.courseID, c.title as CourseTitle, t.firstname || ' ' || t.lastname as ProfessorName
from section sec
join course c on sec.courseID = c.id
join teachers t on sec.crn = t.id;

drop view if exists StudentMajors;
create view StudentMajors as
select s.id as StudentID, sm.major as MajorID, m.title as MajorName
from student s
join student_major sm on s.id = sm.studentID
join major m on sm.major = m.id;

drop view if exists RequiredCourses;
create view RequiredCourses as
select m.id as MajorID, c.id as CourseID, c.title as CourseTitle
from major m
join major_class mc on m.id = mc.majorID
join course c on mc.classID = c.id;

drop view if exists StudentMostVisitedDepartment;
create view StudentMostVisitedDepartment as
select StudentID, DepartmentID, DepartmentName, CourseCount
from (
    select s.id as StudentID, 
           d.id as DepartmentID, 
           d.name as DepartmentName, 
           COUNT(sec.courseID) as CourseCount,
           RANK() OVER (PARTITION BY s.id ORDER BY COUNT(sec.courseID) DESC) as Rank
    from student s
    join student_section ss on s.id = ss.studentID
    join section sec on ss.sectionID = sec.crn
    join course c on sec.courseID = c.id
    join department d on TRIM(LOWER(c.department)) = TRIM(LOWER(d.id)) -- Ensures exact match
    group by s.id, d.id
) RankedDepartments
where Rank = 1;

drop view if exists CurrentClasses;
create view CurrentClasses as
select 
  s.id as StudentID, 
  c.id as CourseID, 
  c.title as CourseTitle, 
  c.hrs as CourseHours,
  sec.term
from student s
join student_section ss on s.id = ss.studentID
join section sec on ss.sectionID = sec.crn
join course c on sec.courseID = c.id;


drop view if exists SITCTeachers;
create view SITCTeachers as
select departmentID, COUNT(*) as TeacherCount
from teachers
group by departmentID;

drop view if exists GradeRequirement;
create view GradeRequirement as
select 
    c.id as CourseID, 
    m.id as MajorID,
    m.deptID as DepartmentID,
    d.name as DepartmentName,
    m.reqtext as RequiredGrade,
    m.gpa as MinimumGPA
from course c
join major m on c.department = m.id
join department d on m.deptID = d.id;

drop view if exists StudentConcentrations;
create view StudentConcentrations as
select
    sm.studentID as StudentID,
    m.id as MajorID,
    m.title as MajorName,
    c.id as ConcentrationID,
    c.title as ConcentrationName,
    c.reqtext as Requirements
from student_major sm
join major m on sm.major = m.id
join concentration c on c.major = m.id;

-- select * from remaining_hours1;
-- select * from remaining_hours2;
-- select * from sections_students;
-- select * from v_and_prereq;
-- select * from v_or_prereq; 
-- select * from v_coreq;
-- select * from MajorConcentrations;
-- select * from StudentClasses;
-- select * from StudentCreditHours;
-- select * from StudentDepartment;
-- select * from CourseProfessor;
-- select * from StudentMajors;
-- select * from RequiredCourses;
-- select * from StudentMostVisitedDepartment;
-- select * from CurrentClasses;
-- select * from SITCTeachers;
-- select * from GradeRequirement;