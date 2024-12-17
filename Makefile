DC = docker compose
EXEC = docker exec -it
APP_FILE = docker-compose.yaml
ENV = --env-file .env
APP = fp-app
POSTGRES = fp-postgres
OLLAMA = fp-ollama
SH = /bin/bash

.PHONY: build
build:
	${DC} -f ${APP_FILE} up --build -d

.PHONY: up
up:
	${DC} -f ${APP_FILE} up

.PHONY: down
down:
	${DC} -f ${APP_FILE} down
.PHONY: app-sh
app-sh:
	${EXEC} -it ${APP} ${SH}

.PHONY: postgres-sh
postgres-sh:
	${EXEC} -it ${POSTGRES} ${SH}

.PHONY: ollama-sh
ollama-sh:
	${EXEC} -it ${OLLAMA} ${SH}