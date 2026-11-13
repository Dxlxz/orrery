# Orrery Frontend Docker Image

[![orrery-frontend docker](https://github.com/Dxlxz/orrery/workflows/orrery-frontend%20docker/badge.svg)](https://github.com/Dxlxz/orrery/actions?query=workflow%3A%22orrery-frontend+docker%22)

Refer to [Orrery Frontend Service](../../orrery-frontend) to have a quick understanding of the architecture and
responsibility of this service for the Orrery.

## Checking out Orrery UI

After starting your Docker container, you can connect to it by typing below into your favorite web browser:

If using React app:

```
http://localhost:9002
```

You can sign in with `orrery` as username and password.

## Build instructions

If you want to build the `orrery-frontend` Docker image yourself, you can run this command from the root directory of the Orrery repository you have locally (using Buildkit):

`DOCKER_BUILDKIT=1 docker build -t your_orrery_frontend -f ./docker/orrery-frontend/Dockerfile .`

Please note the final `.` and that the tag `your_orrery_frontend` is determined by you.
