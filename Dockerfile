FROM nexus.mgmt.health-dev.dwpcloud.uk:5000/dwp/base:develop
EXPOSE 9101
RUN mkdir /opt/ms-html-to-pdfa
ADD ./target /opt/ms-html-to-pdfa
WORKDIR /opt/ms-html-to-pdfa
ENV CLASSPATH=/opt/ms-html-to-pdfa:/opt/ms-html-to-pdfa/.:/opt/ms-html-to-pdfa/*
CMD [ "/bin/java", "uk.gov.dwp.pdfa.application.HtmlToPdfApplication", "server", "/opt/ms-html-to-pdfa/config.yml" ]
