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