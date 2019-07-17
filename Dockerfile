FROM openjdk:11.0.2-jdk-slim
EXPOSE 9101
RUN mkdir /opt/ms-html-to-pdfa

COPY ./target/ms-html-to-pdfa*.jar /opt/ms-html-to-pdfa
COPY ./src/main/properties/dev.yml /opt/ms-html-to-pdfa/config.yml

WORKDIR /opt/ms-html-to-pdfa
ENV CLASSPATH=/opt/ms-html-to-pdfa:/opt/ms-html-to-pdfa/.:/opt/ms-html-to-pdfa/*
CMD [ "/usr/bin/java", "uk.gov.dwp.pdfa.application.HtmlToPdfApplication", "server", "/opt/ms-html-to-pdfa/config.yml" ]
