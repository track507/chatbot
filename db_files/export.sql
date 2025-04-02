.mode column
.headers on
.output database_export.txt

-- Exporting Tables
.print 'Table: course'
select * from course;

.print 'Table: department'
select * from department;

.print 'Table: major'
select * from major;

.print 'Table: major_class'
select * from major_class;

.print 'Table: or_prereq'
select * from or_prereq;

.print 'Table: section'
select * from section;

.print 'Table: student'
select * from student;

.print 'Table: student_major'
select * from student_major;

.print 'Table: student_section'
select * from student_section;

.print 'Table: teachers'
select * from teachers;

.print 'Table: and_prereq'
select * from and_prereq;

.print 'Table: college'
select * from college;

.print 'Table: concentration'
select * from concentration;

.print 'Table: coreq'
select * from coreq;

-- Exporting Views 
.print 'View: studentmajors'
select * from studentmajors;

.print 'View: studentconcentrations'
select * from studentconcentrations;

.print 'View: currentclasses'
select * from currentclasses;

.print 'View: studentcredithours'
select * from studentcredithours;

.print 'View: studentdepartment'
select * from studentdepartment;

.print 'View: remaining_hours1'
select * from remaining_hours1;

.output stdout
