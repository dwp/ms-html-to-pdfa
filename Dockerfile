FROM gcr.io/distroless/java17@sha256:9595fd074976b36fbaa8feec02c2e15b93fd3a6c424563ef364a1c28b2eca30d
EXPOSE 9101

COPY ./target/ms-html-to-pdfa*.jar /ms-html-to-pdfa.jar
COPY ./src/main/properties/dev.yml /config.yml

COPY --from=pik94420.live.dynatrace.com/linux/oneagent-codemodules:java / /
ENV LD_PRELOAD /opt/dynatrace/oneagent/agent/lib64/liboneagentproc.so

ENTRYPOINT [ "java", "-jar", "/ms-html-to-pdfa.jar", "server", "/config.yml" ]
