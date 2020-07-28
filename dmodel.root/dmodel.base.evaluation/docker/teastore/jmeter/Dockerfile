FROM openjdk:13

MAINTAINER David Monschein

# Basic installations
RUN \
	yum -y update &&\
	yum -y install unzip && \
	yum -y install nano && \
	yum -y install git && \
	yum -y install curl wget && \
	yum -y install dos2unix

RUN mkdir /jmeter \
    && cd /jmeter/ \
    && wget https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-5.2.1.tgz \
    && tar -xvzf apache-jmeter-5.2.1.tgz \
    && rm apache-jmeter-5.2.1.tgz
	
ENV JMETER_HOME /jmeter/apache-jmeter-5.2.1/

# Add Jmeter to the Path
ENV PATH $JMETER_HOME/bin:$PATH

ADD start.sh /start.sh 
ADD test.jmx /test.jmx

RUN dos2unix /start.sh

#Make the scripts executable 
RUN chmod +x /*.sh

ENTRYPOINT ["/start.sh"] 
CMD []
