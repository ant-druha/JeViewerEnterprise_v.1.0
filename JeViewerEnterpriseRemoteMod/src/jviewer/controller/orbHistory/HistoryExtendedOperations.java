package jviewer.controller.orbHistory;


/**
* jviewer/controller/orbHistory/HistoryExtendedOperations.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from ControllerHistory.idl
* Sunday, November 18, 2012 7:59:07 PM MSK
*/

public interface HistoryExtendedOperations  extends jviewer.controller.orbHistory.HistoryOperations
{
  boolean loginExt (String login, String password, jviewer.controller.orbHistory.HistoryExtendedPackage.ClientInfoHolder clientInfo, org.omg.CORBA.StringHolder strResult);
  void shutdown ();
} // interface HistoryExtendedOperations