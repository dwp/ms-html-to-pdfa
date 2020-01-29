FROM gcr.io/distroless/java:11
EXPOSE 9101

COPY ./target/ms-html-to-pdfa*.jar /ms-html-to-pdfa.jar
COPY ./src/main/properties/dev.yml /config.yml

ENTRYPOINT [ "java", "-jar", "/ms-html-to-pdfa.jar", "server", "/config.yml" ]
