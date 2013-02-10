CREATE USER jviewer 
    IDENTIFIED BY jviewer
	default tablespace USERS;

ALTER USER jviewer QUOTA UNLIMITED ON users; 
grant create session to jviewer;
grant debug connect session to jviewer;

-- Create table employees
create table JVIEWER.employees
(
  employee_id number not null,
  first_name  varchar2(24) not null,
  last_name   varchar2(32) not null,
  login       varchar2(16) not null,
  password    varchar2(16)
)
tablespace USERS
  storage
  (
    initial 64K
    minextents 1
    maxextents unlimited
  );
-- Create/Recreate primary, unique and foreign key constraints 
alter table JVIEWER.employees
  add constraint employee_id primary key (EMPLOYEE_ID);
alter table JVIEWER.employees								-- add this!!!
  add constraint login_unic
  unique (login);
  
-- Create table employee_history
create table JVIEWER.employee_history
(
  id           number not null,
  position     varchar2(24) not null,
  manager      number,
  hire_date    date not null,
  dismiss_date date,
  employee_id  number not null
)
tablespace USERS
  storage
  (
    initial 64K
    minextents 1
    maxextents unlimited
  );
-- Create/Recreate primary, unique and foreign key constraints 
alter table JVIEWER.employee_history
  add constraint emp_hist_pk primary key (ID);
alter table JVIEWER.employee_history
  add constraint emp_hist_fk foreign key (EMPLOYEE_ID)
  references JVIEWER.employees (EMPLOYEE_ID);
-- Create/Recreate check constraints 
alter table JVIEWER.employee_history
  add constraint manager
  check ( manager > 0);
alter table JVIEWER.employee_history
  add constraint dismiss
  check ( dismiss_date >= hire_date);
  
  
-- Grant/Revoke object privileges 
grant select, insert, update, delete, alter on JVIEWER.employee_history to jviewer;  
grant select, insert, update, delete, alter on JVIEWER.employees to jviewer;

-- Create table
create table JVIEWER.application
(
  application_id number not null,
  name           varchar2(35) not null,
  type           number
)
;
-- Create/Recreate primary, unique and foreign key constraints 
alter table JVIEWER.application
  add constraint applicationPk primary key (APPLICATION_ID);
  
  -- Create table
create table JVIEWER.sessions
(
  session_id     number not null,
  employee_id    number not null,
  login_date     timestamp default current_timestamp not null,
  logout_date    timestamp,
  is_online      number default 1 not null,
  server_name    varchar2(20),
  client_ip      varchar2(15),
  application_id number,
  app_version    varchar2(25),
  app_info       varchar2(60)
)
;
-- Add/modify columns 
--alter table JVIEWER.SESSIONS modify app_info VARCHAR2(60);

-- Create/Recreate primary, unique and foreign key constraints 
alter table JVIEWER.sessions
  add constraint sessionsPk primary key (SESSION_ID);
alter table JVIEWER.sessions
  add constraint sess_empFk foreign key (EMPLOYEE_ID)
  references JVIEWER.employees (EMPLOYEE_ID);
alter table JVIEWER.sessions
  add constraint sess_appFk foreign key (APPLICATION_ID)
  references JVIEWER.application (APPLICATION_ID);
-- Create/Recreate check constraints 
alter table JVIEWER.sessions
  add constraint is_online_chk
  check (is_online between 0 and 1);


-- ========================================
-- employees_seq
create sequence JVIEWER.employees_seq
 START WITH     1
 INCREMENT BY   1
 NOCACHE
 NOCYCLE;
 -- emp_history_seq
create sequence JVIEWER.emp_history_seq
 START WITH     1
 INCREMENT BY   1
 NOCACHE
 NOCYCLE;
-- sessions_seq
create sequence JVIEWER.sessions_seq
 START WITH     1
 INCREMENT BY   1
 NOCACHE
 NOCYCLE;
-- employeesBeforeInsert trigger
create or replace trigger employeesBeforeInsert
  before insert on JVIEWER.employees
  for each row
begin
  select JVIEWER.employees_seq.nextval into :new.employee_id from dual;
end employeesBeforeInsert;
/
-- emp_historyBeforeInsert trigger
create or replace trigger emp_historyBeforeInsert
  before insert on JVIEWER.employee_history
  for each row
declare
  -- local variables here
begin
  select JVIEWER.emp_history_seq.nextval into :new.id from dual;
end emp_historyBeforeInsert;
/  
-- sessionsBeforeInsert trigger
create or replace trigger sessionsBeforeInsert
  before insert on JVIEWER.sessions
  for each row
begin
  select JVIEWER.sessions_seq.nextval into :new.session_id from dual;
end sessionsBeforeInsert;
/