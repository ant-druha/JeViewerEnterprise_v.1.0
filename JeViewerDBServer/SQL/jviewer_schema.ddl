CREATE USER jviewer
    IDENTIFIED BY jviewer
	default tablespace USERS;

ALTER USER jviewer QUOTA UNLIMITED ON users;
grant create session to jviewer;
-- grant debug connect session to jviewer;

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




-- =========================================================================
-- DB logic below
-- =========================================================================

-- ======================================================
-- START OF JVIEWER.ClientUtils package here
-- ======================================================
create or replace package jviewer.ClientUtils is

  -- Author  : ANDREY
  -- Created : 15.11.2012 23:07:05
  -- Purpose : Client bussines logic goes here

  -- Public type declarations
  type ResultSetType is ref cursor;

  -- Public constant declarations
  --  <ConstantName> constant <Datatype> := <Value>;

  -- Public variable declarations
  --  <VariableName> <Datatype>;

  -- Public function and procedure declarations
  function ClientLogin(p_login          in varchar2,
                       p_password       in varchar2,
                       p_server_name    in varchar2 := null,
                       p_client_ip      in varchar2 := null,
                       p_application_id in varchar2 := null,
                       p_app_version    in varchar2 := null,
                       p_app_info       in varchar2 := null,
                       p_outMsg         out varchar2,
                       p_outSessionId   out integer,
                       p_client_id      out integer) return integer;

  function ClientLogout(p_employee_id in integer,
                        p_retMsg      out varchar2) return integer;

  function GetClientHistory(p_client_id in integer) return ResultSetType;

end ClientUtils;
/
create or replace package body jviewer.ClientUtils is

  -- Private type declarations
  --  type < TypeName > is < Datatype >;

  -- Private constant declarations
  --  < ConstantName > constant < Datatype > := < Value >;

  -- Private variable declarations
  --  < VariableName > < Datatype >;

  -- Function and procedure implementations

  --function ClientLogin
  function ClientLogin(p_login          in varchar2,
                       p_password       in varchar2,
                       p_server_name    in varchar2 := null,
                       p_client_ip      in varchar2 := null,
                       p_application_id in varchar2 := null,
                       p_app_version    in varchar2 := null,
                       p_app_info       in varchar2 := null,
                       p_outMsg         out varchar2,
                       p_outSessionId   out integer,
                       p_client_id      out integer) return integer is

    v_cnt   integer default 0;
    v_login jviewer.employees.login%type;

  begin
    select employee_id, login
      into p_client_id, v_login
      from jviewer.employees
     where login like p_login
       and password like p_password;
    if p_client_id = 0 then
      p_outMsg := 'Invalid login or password';
      return 0;
    end if;

    if p_client_id <> 0 then
      -- check if client with such login already logged in
      select count(session_id)
        into v_cnt
        from jviewer.sessions
       where employee_id = p_client_id
         and is_online = 1;

      if v_cnt > 0 then
        p_outMsg := 'Already logged in!';
        return 0;
      else
        -- TODO: make a trigger to check no more than one online clients in sessions table !!!
        insert into jviewer.sessions
          (employee_id, is_online, server_name, client_ip, application_id,
           app_version, app_info)
        values
          (p_client_id, 1, p_server_name, p_client_ip, p_application_id,
           p_app_version, p_app_info);

        if sql%rowcount > 0 then
          commit;
          select session_id
            into p_outSessionId
            from sessions
           where employee_id = p_client_id
             and is_online = 1;
          p_outMsg := 'Login successful';
          return 1;
        else
          rollback;
          p_outMsg := 'Some error occured while inserting in sessions table ...';
          return 0;
        end if;
      end if;

    end if;
  exception
    when others then
      rollback;
      p_outMsg := 'Some error occured while inserting in sessions table ...';
	  return 0;
  end;

  -- function ClientLogout
  function ClientLogout(p_employee_id in integer,
                        p_retMsg      out varchar2) return integer is
    v_cnt integer default 0;
  begin
    select count(session_id)
      into v_cnt
      from jviewer.sessions
     where employee_id = p_employee_id
       and is_online = 1;
    if v_cnt = 1 then
      update sessions
         set is_online = 0, logout_date = current_timestamp
       where employee_id = p_employee_id
         and is_online = 1;
      if sql%rowcount > 0 then
        commit;
        p_retMsg := 'Logout successful';
        return 1;
      else
        rollback;
        p_retMsg := 'Some error ocured while updating sessions table ...';
        return 0;
      end if;
    end if;
    p_retMsg := 'Client was not login';
    return 0;
  end;

  -- function
  function GetClientHistory(p_client_id in integer) return ResultSetType is
    cResultSet ResultSetType;
  begin

    open cResultSet for
      select id, position, manager, hire_date, dismiss_date, employee_id
        from jviewer.employee_history
       where employee_id = p_client_id;

    return(cResultSet);
  end;

end ClientUtils;
-- ======================================================
-- END OF JVIEWER.ClientUtils package here
-- ======================================================
/
-- execute privelege
grant execute on jviewer.clientutils to jviewer;

-- ======================================================
-- START OF JVIEWER.ClientUtils package functions and procedures here
-- ======================================================

-- ======================================================
-- END OF JVIEWER.ClientUtils package functions and procedures here
-- ======================================================