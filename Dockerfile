FROM gcr.io/distroless/java11@sha256:f88c393a67fff3f9b599eca0e90350ee27e96d3803cbc7742ec23de6a9d6dd7d
EXPOSE 9101

COPY ./target/ms-html-to-pdfa*.jar /ms-html-to-pdfa.jar
COPY ./src/main/properties/dev.yml /config.yml

ENTRYPOINT [ "java", "-jar", "/ms-html-to-pdfa.jar", "server", "/config.yml" ]
