FROM gcr.io/distroless/java11@sha256:5814a55f4ec3b2cebedab0e35f6073dbaa0554393026a333a905bf2578d5a481
EXPOSE 9101

COPY ./target/ms-html-to-pdfa*.jar /ms-html-to-pdfa.jar
COPY ./src/main/properties/dev.yml /config.yml

ENTRYPOINT [ "java", "-jar", "/ms-html-to-pdfa.jar", "server", "/config.yml" ]
