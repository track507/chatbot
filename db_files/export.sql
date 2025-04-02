.mode column
.headers on
.output database_export.txt

-- examples

-- Exporting Tables
.print 'Table: course'
SELECT * FROM course;

.print 'Table: department'
SELECT * FROM department;

.print 'Table: major'
SELECT * FROM major;

.print 'Table: major_class'
SELECT * FROM major_class;

.print 'Table: or_prereq'
SELECT * FROM or_prereq;

.print 'Table: section'
SELECT * FROM section;

.print 'Table: student'
SELECT * FROM student;

.print 'Table: student_major'
SELECT * FROM student_major;

.print 'Table: student_section'
SELECT * FROM student_section;

.print 'Table: teachers'
SELECT * FROM teachers;

.print 'Table: and_prereq'
SELECT * FROM and_prereq;

.print 'Table: college'
SELECT * FROM college;

.print 'Table: concentration'
SELECT * FROM concentration;

.print 'Table: coreq'
SELECT * FROM coreq;

-- Exporting Views
.print 'View: remaining_hours1'
SELECT * FROM remaining_hours1;

.print 'View: remaining_hours2'
SELECT * FROM remaining_hours2;

.print 'View: sections_students'
SELECT * FROM sections_students;

.print 'View: v_and_prereq'
SELECT * FROM v_and_prereq;

.print 'View: v_or_prereq'
SELECT * FROM v_or_prereq;

.print 'View: v_coreq'
SELECT * FROM v_coreq;

.print 'View: MajorConcentrations'
SELECT * FROM MajorConcentrations;

.print 'View: StudentClasses'
SELECT * FROM StudentClasses;

.print 'View: StudentCreditHours'
SELECT * FROM StudentCreditHours;

.print 'View: StudentDepartment'
SELECT * FROM StudentDepartment;

.print 'View: CourseProfessor'
SELECT * FROM CourseProfessor;

.print 'View: StudentMajors'
SELECT * FROM StudentMajors;

.print 'View: RequiredCourses'
SELECT * FROM RequiredCourses;

.print 'View: StudentMostVisitedDepartment'
SELECT * FROM StudentMostVisitedDepartment;

.print 'View: CurrentClasses'
SELECT * FROM CurrentClasses;

.print 'View: SITCTeachers'
SELECT * FROM SITCTeachers;

.print 'View: GradeRequirement'
SELECT * FROM GradeRequirement;

.output stdout