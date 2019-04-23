FROM 172.16.3.50:8443/library/jdk:8
  
COPY target/bdos-0.0.1-SNAPSHOT.jar /root

#CMD java -jar /root/bdos-0.0.1-SNAPSHOT.jar