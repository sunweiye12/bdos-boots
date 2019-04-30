FROM 172.16.3.216:8443/library/tomcat:8.5.38
  
COPY target/bdos.war /usr/local/tomcat/webapps/

CMD catalina.sh run