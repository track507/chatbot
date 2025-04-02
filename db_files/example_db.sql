PRAGMA foreign_keys=on;

-- Delete in certain order
DROP TABLE IF EXISTS concentration;
DROP TABLE IF EXISTS student_major;
DROP TABLE IF EXISTS coreq;
DROP TABLE IF EXISTS or_prereq;
DROP TABLE IF EXISTS and_prereq;
DROP TABLE IF EXISTS major_class;
DROP TABLE IF EXISTS student_section;
DROP TABLE IF EXISTS section;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS teachers;
DROP TABLE IF EXISTS major;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS department;
DROP TABLE IF EXISTS college;


CREATE TABLE college ( id TEXT PRIMARY KEY, name TEXT );
CREATE TABLE department ( id TEXT NOT NULL, name TEXT NOT NULL, collegeID TEXT DEFAULT NULL, PRIMARY KEY (id), FOREIGN KEY (collegeID) REFERENCES college (id));
create table course (id text, department text, title text, num int, hrs int, primary key(id), foreign key (department) REFERENCES department(id));
create table major (id text, title text, deptID text, reqtext text, hrs int, gpa float, primary key(id), foreign key (deptID) references department(id));
CREATE TABLE teachers (id INT PRIMARY KEY, firstname TEXT, lastname TEXT, departmentID TEXT, adjunct INT, FOREIGN KEY (departmentID) REFERENCES department(id));
create table student(id int primary key, firstname text, lastname text);
create table section (crn int, max int, room text, courseID text, term text, startdate date, enddate date, days text, primary key(crn), foreign key (courseID) references course(id));
CREATE TABLE student_section (studentID INTEGER, sectionID INTEGER, grade REAL, PRIMARY KEY (studentID, sectionID),FOREIGN KEY (sectionID) REFERENCES section(crn), FOREIGN KEY (studentID) REFERENCES student(id));
create table major_class(majorID int, classID int, FOREIGN KEY(majorID) REFERENCES major(id), FOREIGN KEY(classID) REFERENCES course(id));
CREATE TABLE and_prereq (course TEXT,prereq TEXT,PRIMARY KEY (course, prereq),FOREIGN KEY (course) REFERENCES course(id),FOREIGN KEY (prereq) REFERENCES course(id));
CREATE TABLE or_prereq (course TEXT,prereq TEXT,PRIMARY KEY (course, prereq),FOREIGN KEY (course) REFERENCES course(id), FOREIGN KEY (prereq) REFERENCES course(id));
CREATE TABLE coreq (course TEXT, prereq TEXT, PRIMARY KEY (course, prereq), FOREIGN KEY (course) REFERENCES course(id), FOREIGN KEY (prereq) REFERENCES course(id));
create table student_major(studentID INTEGER, major TEXT, PRIMARY KEY (studentID, major), FOREIGN KEY (major) REFERENCES major(id), FOREIGN KEY (studentID) REFERENCES student(id));
create table concentration (id text primary key, major text, title text, reqtext text, foreign key(major) references major(id));