# Git Service

Receives an OpenAPI Spec from Authoring Tool, checks it into Git, and Initiates the code Generate/Deploy pipeline.

Environment variables:
```
export GIT_URL=http://gogs-labs-ci.apps.cluster.com/eformat/generated-code.git
export GIT_USERNAME=<username>
export GIT_PASSWORD=<password>

mvn quarkus:dev
```

