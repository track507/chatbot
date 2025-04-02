select * from college;
select * from department;
select * from course;
select * from major;
select * from teachers;
select * from student;
select * from section;
select * from student_section;
select * from major_class;
select * from and_prereq;
select * from or_prereq;
select * from coreq;
select * from student_major;
select * from concentration;

-- BELOW ARE THE FOLLOWING AND ARE REQUIRED TO RUN THE PROGRAM
-- i forgot that the creation of the user_info.json was based off these views.

-- view: StudentMajors
drop view if exists StudentMajors;
create view StudentMajors as
select s.id as StudentID, sm.major as MajorID, m.title as MajorName
from student s
join student_major sm on s.id = sm.studentID
join major m on sm.major = m.id;

-- view: StudentConcentrations
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

-- view: CurrentClasses
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

-- view: StudentCreditHours
drop view if exists StudentCreditHours;
create view StudentCreditHours as
select s.id as StudentID, SUM(c.hrs) as TotalCreditHours
from student s
join student_section ss on s.id = ss.studentID
join section sec on ss.sectionID = sec.crn
join course c on sec.courseID = c.id
group by s.id;

-- view: StudentDepartment
drop view if exists StudentDepartment;
create view StudentDepartment as
select s.id as StudentID, d.id as DepartmentID, d.name as DepartmentName
from student s
join student_major sm on s.id = sm.studentID
join major m on sm.major = m.id
join department d on m.deptID = d.id;

-- view: remaining_hours1
drop view if exists remaining_hours1;
create view remaining_hours1 as
select s.id as studentid, sm.major as majorid, 
       m.hrs as total_required,
       coalesce(sum(distinct c.hrs), 0) as completed_hours,
       m.hrs - coalesce(sum(distinct c.hrs), 0) as remaining_hours
from student s
join student_major sm on s.id = sm.studentid
join major m on sm.major = m.id
left join student_section ss on s.id = ss.studentid
left join section sec on ss.sectionid = sec.crn
left join course c on sec.courseid = c.id
left join major_class mc on c.id = mc.classid and mc.majorid = sm.major
group by s.id, sm.major;