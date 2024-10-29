ARG APP_INSIGHTS_AGENT_VERSION=3.6.1

# Application image

FROM hmctspublic.azurecr.io/base/java:21-distroless

# Change to non-root privilege
USER hmcts

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/pcq-loader.jar /opt/app/

EXPOSE 4556
CMD [ "pcq-loader.jar" ]
