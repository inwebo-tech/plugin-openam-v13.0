inWebo OpenAm v13.0.0 Custom Authentication Module
==================================================
 
Requirements
------------
 
1. OpenAM v13.0.0
1. SSOAdminTools
1. [Tomcat 7](http://apache.mediamirrors.org/tomcat/tomcat-7/v7.0.90/bin/apache-tomcat-7.0.90.tar.gz)
1. [Oracle Sun JDK 1.7](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html)
1. [(JCE) Unlimited Strength Jurisdiction Policy Files](http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html)
 
Building from Source
--------------------
  
  ```bash
  $ git clone https://github.com/inwebo-tech/plugin-openam-v13.0.git
  $ cd plugin-openam-v13.0 
  $ ./mvnw clean package
  ```
  
Installation
------------
   
  ```bash
  $ sudo unzip iw-openam-auth-*.zip -d /tmp/openam-ui-inwebo
  $ sudo cp /tmp/openam-ui-inwebo/edit-webapp/WEB-INF/lib/*.jar /path/to/tomcat/webapps/openam/WEB-INF/lib/
  $ sudo cp /tmp/openam-ui-inwebo/edit-webapp/config/auth/default/* /path/to/tomcat/webapps/openam/config/auth/default/
  $ sudo tar cvjf xui-backup-orig.tar.bz /path/to/tomcat/webapps/openam/XUI/ 
  $ sudo rm -rf /path/to/tomcat/webapps/openam/XUI/*
  $ sudo cp -R /tmp/openam-ui-inwebo/edit-webapp/XUI/* /path/to/webapps/openam/XUI/
  $ sudo cp /tmp/openam-ui-inwebo/edit-webapp/js/* /path/to/webapps/openam/js/
  $ sudo echo password > /tmp/pwd.txt
  $ sudo chmod 400 /tmp/pwd.txt
  $ sudo ssoadm create-svc -u amadmin --password-file /tmp/pwd.txt --xmlfile /tmp/openam-ui-inwebo/config/amAuthInWeboAuth.xml -v
  $ sudo ssoadm register-auth-module -u amadmin -f /tmp/pwd.txt -a org.forgerock.openam.inwebo.InWeboAuth -v
  $ sudo /etc/init.d/tomcat restart
  ```
 
Quicker UI Customization
-------------------------
 
1. Go to Admin console and Login as 'amadmin'.
1. Go to Configuration->Servers and Sites->Server Name->Advanced.
1. set 'org.forgerock.openam.core.resource.lookup.cache.enabled' to false.
 
:warning: **For production servers, leave this set to the default, true**
 
How to debug
------------
 
1. Go to http(s)://{OPENAM_HOST}/openam/Debug.jsp
1. Click the Category dropdown.
1. Select Configuration and set level to Message.
1. Click Submit.
1. Click confirm.

Follow the steps for Authentication, CoreSystem. From there go to ~/openam/openam/debug/ and run tail -f * to tail output from all the logs in one.