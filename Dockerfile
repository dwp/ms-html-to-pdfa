FROM gcr.io/distroless/java17@sha256:052076466984fd56979c15a9c3b7433262b0ad9aae55bc0c53d1da8ffdd829c3
EXPOSE 9101

COPY ./target/ms-html-to-pdfa*.jar /ms-html-to-pdfa.jar
COPY ./src/main/properties/dev.yml /config.yml

ENTRYPOINT [ "java", "-jar", "/ms-html-to-pdfa.jar", "server", "/config.yml" ]
