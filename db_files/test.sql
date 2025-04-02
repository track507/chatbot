PRAGMA foreign_keys = ON;
-- Insert colleges
INSERT INTO college (id, name) VALUES 
('COL1', 'College of Arts and Sciences'),
('COL2', 'College of Engineering'),
('COL3', 'College of Business');

-- Insert departments
INSERT INTO department (id, name, collegeID) VALUES 
('DEPT1', 'Computer Science', 'COL2'),
('DEPT2', 'Biology', 'COL1'),
('DEPT3', 'Business Administration', 'COL3');

-- Insert courses
INSERT INTO course (id, department, title, num, hrs) VALUES 
-- Computer Science courses
('CS101', 'DEPT1', 'Introduction to Programming', 101, 3),
('CS120', 'DEPT1', 'Data Structures', 120, 4),
('CS130', 'DEPT1', 'Algorithms', 130, 4),
('CS220', 'DEPT1', 'Database Systems', 220, 4),
('CS240', 'DEPT1', 'Computer Networks', 240, 3),
-- Biology courses
('BIO101', 'DEPT2', 'General Biology I', 101, 4),
('BIO102', 'DEPT2', 'General Biology II', 102, 4),
('BIO112', 'DEPT2', 'Cell Biology', 112, 4),
('BIO201', 'DEPT2', 'Genetics', 201, 4),
-- Business courses
('BUS101', 'DEPT3', 'Principles of Management', 101, 3),
('BUS201', 'DEPT3', 'Financial Accounting', 201, 3),
('BUS202', 'DEPT3', 'Marketing Principles', 202, 3),
('BUS301', 'DEPT3', 'Business Law', 301, 3);

-- Insert prerequisites
-- CS220 requires both CS120 AND CS130
INSERT INTO and_prereq (course, prereq) VALUES 
('CS220', 'CS120'),
('CS220', 'CS130');

-- BIO112 requires either BIO101 OR BIO102
INSERT INTO or_prereq (course, prereq) VALUES 
('BIO112', 'BIO101'),
('BIO112', 'BIO102');

-- Insert majors
INSERT INTO major (id, title, deptID, reqtext, hrs, gpa) VALUES 
('MAJ1', 'Computer Science', 'DEPT1', 'Complete all core CS courses', 120, 2.5),
('MAJ2', 'Biology', 'DEPT2', 'Complete biology core and electives', 124, 2.5),
('MAJ3', 'Business Administration', 'DEPT3', 'Complete business core', 120, 2.0);

-- Insert teachers
-- Department 1 (Computer Science) - 3 teachers
INSERT INTO teachers (id, firstname, lastname, departmentID, adjunct) VALUES 
(101, 'John', 'Smith', 'DEPT1', 0),
(102, 'Alice', 'Johnson', 'DEPT1', 1),
(103, 'Robert', 'Williams', 'DEPT1', 0);

-- Department 2 (Biology) - 5 teachers
INSERT INTO teachers (id, firstname, lastname, departmentID, adjunct) VALUES 
(201, 'Emily', 'Brown', 'DEPT2', 0),
(202, 'Michael', 'Davis', 'DEPT2', 1),
(203, 'Sarah', 'Miller', 'DEPT2', 0),
(204, 'David', 'Wilson', 'DEPT2', 1),
(205, 'Jennifer', 'Moore', 'DEPT2', 0);

-- Department 3 (Business) - 6 teachers
INSERT INTO teachers (id, firstname, lastname, departmentID, adjunct) VALUES 
(301, 'James', 'Taylor', 'DEPT3', 0),
(302, 'Jessica', 'Anderson', 'DEPT3', 1),
(303, 'Thomas', 'Thomas', 'DEPT3', 0),
(304, 'Elizabeth', 'Jackson', 'DEPT3', 1),
(305, 'Charles', 'White', 'DEPT3', 0),
(306, 'Patricia', 'Harris', 'DEPT3', 1);

-- Insert sections
INSERT INTO section (crn, max, room, courseID, term, startdate, enddate, days) VALUES 
-- Computer Science sections
(1001, 30, 'CS101', 'CS101', 'Fall 2023', '2023-08-21', '2023-12-15', 'MWF'),
(1002, 25, 'CS120', 'CS120', 'Fall 2023', '2023-08-21', '2023-12-15', 'TR'),
(1003, 25, 'CS130', 'CS130', 'Fall 2023', '2023-08-21', '2023-12-15', 'MWF'),
(1004, 20, 'CS220', 'CS220', 'Spring 2024', '2024-01-16', '2024-05-10', 'TR'),
(1005, 30, 'CS240', 'CS240', 'Spring 2024', '2024-01-16', '2024-05-10', 'MWF'),
-- Biology sections
(2001, 40, 'BIO101', 'BIO101', 'Fall 2023', '2023-08-21', '2023-12-15', 'MWF'),
(2002, 40, 'BIO102', 'BIO102', 'Spring 2024', '2024-01-16', '2024-05-10', 'MWF'),
(2003, 30, 'BIO112', 'BIO112', 'Spring 2024', '2024-01-16', '2024-05-10', 'TR'),
(2004, 25, 'BIO201', 'BIO201', 'Fall 2023', '2023-08-21', '2023-12-15', 'TR'),
-- Business sections
(3001, 50, 'BUS101', 'BUS101', 'Fall 2023', '2023-08-21', '2023-12-15', 'MWF'),
(3002, 45, 'BUS201', 'BUS201', 'Spring 2024', '2024-01-16', '2024-05-10', 'TR'),
(3003, 45, 'BUS202', 'BUS202', 'Fall 2023', '2023-08-21', '2023-12-15', 'TR'),
(3004, 40, 'BUS301', 'BUS301', 'Spring 2024', '2024-01-16', '2024-05-10', 'MWF');

-- Insert students
INSERT INTO student (id, firstname, lastname) VALUES 
(1, 'Alex', 'Johnson'),
(2, 'Maria', 'Garcia'),
(3, 'David', 'Lee');

-- Student 1: Computer Science major with Software Engineering concentration
INSERT INTO student_major (studentID, major) VALUES (1, 'MAJ1');
INSERT INTO concentration (id, major, title, reqtext) VALUES 
('STU1_CON1', 'MAJ1', 'Software Engineering', 'Custom plan for Alex Johnson');

-- Student 2: Biology major with Molecular Biology concentration
INSERT INTO student_major (studentID, major) VALUES (2, 'MAJ2');
INSERT INTO concentration (id, major, title, reqtext) VALUES 
('STU2_CON1', 'MAJ2', 'Molecular Biology', 'Custom plan for Maria Garcia');

-- Student 3: Business major with both Finance and Marketing concentrations
INSERT INTO student_major (studentID, major) VALUES (3, 'MAJ3');
INSERT INTO concentration (id, major, title, reqtext) VALUES 
('STU3_CON1', 'MAJ3', 'Finance', 'Custom plan for David Lee - Finance'),
('STU3_CON2', 'MAJ3', 'Marketing', 'Custom plan for David Lee - Marketing');

-- Enroll students in sections
-- Student 1 (CS major) enrolled in 3 courses
INSERT INTO student_section (studentID, sectionID, grade) VALUES 
(1, 1001, 3.5), -- CS101
(1, 1002, 3.0), -- CS120
(1, 1003, 4.0); -- CS130

-- Student 2 (Biology major) enrolled in 4 courses
INSERT INTO student_section (studentID, sectionID, grade) VALUES 
(2, 2001, 3.7), -- BIO101
(2, 2002, 3.3), -- BIO102
(2, 2003, 3.0), -- BIO112
(2, 2004, 4.0); -- BIO201

-- Student 3 (Business major) enrolled in 2 courses
INSERT INTO student_section (studentID, sectionID, grade) VALUES 
(3, 3001, 3.0), -- BUS101
(3, 3003, 3.5); -- BUS202


-- Add SITC department
INSERT INTO department (id, name, collegeID) VALUES 
('DEPT4', 'SITC', 'COL2');

-- Add CS375 course
INSERT INTO course (id, department, title, num, hrs) VALUES 
('CS375', 'DEPT4', 'Advanced Database Systems', 375, 3);

-- Add SITC teachers (6 teachers)
INSERT INTO teachers (id, firstname, lastname, departmentID, adjunct) VALUES 
(401, 'William', 'Thompson', 'DEPT4', 0),
(402, 'Lisa', 'Martin', 'DEPT4', 1),
(403, 'Richard', 'Clark', 'DEPT4', 0),
(404, 'Susan', 'Rodriguez', 'DEPT4', 1),
(405, 'Daniel', 'Lewis', 'DEPT4', 0),
(406, 'Nancy', 'Lee', 'DEPT4', 1);

-- Add SITC major
INSERT INTO major (id, title, deptID, reqtext, hrs, gpa) VALUES 
('MAJ4', 'Information Technology', 'DEPT4', 'Complete all core IT courses', 124, 2.5);

-- Add SITC concentrations
INSERT INTO concentration (id, major, title, reqtext) VALUES 
('CON6', 'MAJ4', 'Database Systems', 'Complete database track courses'),
('CON7', 'MAJ4', 'Cybersecurity', 'Complete cybersecurity track courses'),
('CON8', 'MAJ4', 'Web Development', 'Complete web development track');

-- Add CS375 section
INSERT INTO section (crn, max, room, courseID, term, startdate, enddate, days) VALUES 
(4001, 25, 'SITC301', 'CS375', 'Spring 2024', '2024-01-16', '2024-05-10', 'TR');

-- Update student 1 to be in SITC department with IT major
UPDATE student_major SET major = 'MAJ4' WHERE studentID = 1;
DELETE FROM concentration WHERE id = 'STU1_CON1';
INSERT INTO concentration (id, major, title, reqtext) VALUES 
('STU1_CON1', 'MAJ4', 'Database Systems', 'Custom plan for Alex Johnson');

-- Enroll student 1 in CS375
INSERT INTO student_section (studentID, sectionID, grade) VALUES 
(1, 4001, NULL); -- Currently enrolled, no grade yet

-- Add more data for demonstration
INSERT INTO student_section (studentID, sectionID, grade) VALUES 
(1, 1005, 3.7), -- CS240
(1, 3001, 4.0); -- BUS101 (elective)