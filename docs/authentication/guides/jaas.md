---
description: "Configure JAAS authentication in Orrery to integrate with file-based, LDAP, or custom Java identity providers."
---

# JaaS Authentication

## Overview

The Orrery frontend server comes with support for plugging in [JaaS](https://docs.oracle.com/javase/7/docs/technotes/guides/security/jaas/JAASRefGuide.html) modules.
This allows you to use a custom authentication protocol to log your users into Orrery.

By default, we in include sample configuration of a file-based username / password authentication module ([PropertyFileLoginModule](http://archive.eclipse.org/jetty/8.0.0.M3/apidocs/org/eclipse/jetty/plus/jaas/spi/PropertyFileLoginModule.html))
that is configured with a single username / password combination: orrery - orrery.

To change or extend the default behavior, you have multiple options, each dependent on which deployment environment you're operating in.

### Modify user.props file directly (Local Testing)

The first option for customizing file-based users is to modify the file `orrery-frontend/app/conf/user.props` directly.
Once you've added your desired users, you can simply run `./dev.sh` or `./orrery-frontend/run-local-frontend` to validate your
new users can log in.

### Mount a custom user.props file (Docker Compose)

By default, the `orrery-frontend` container will look for a file called `user.props` mounted at the container path
`/orrery-frontend/conf/user.props`. If you wish to launch this container with a custom set of users, you'll need to override the default
file mounting when running using `docker-compose`.

To do so, change the `orrery-frontend-react` service in the docker-compose.yml file containing it to include the custom file:

```
orrery-frontend-react:
    build:
      context: ../
      dockerfile: docker/orrery-frontend/Dockerfile
    image: acryldata/orrery-frontend-react:${ORRERY_VERSION:-quickstart}
    env_file: orrery-frontend/env/docker.env
    hostname: orrery-frontend-react
    container_name: orrery-frontend-react
    ports:
      - "9002:9002"
    depends_on:
      - orrery-gms
    volumes:
      - ./my-custom-dir/user.props:/orrery-frontend/conf/user.props
```

And then run `docker-compose up` against your compose file.

## Custom JaaS Configuration

In order to change the default JaaS module configuration, you will have to launch the `orrery-frontend-react` container with the custom `jaas.conf` file mounted as a volume
at the location `/orrery-frontend/conf/jaas.conf`.

To do so, change the `orrery-frontend-react` service in the docker-compose.yml file containing it to include the custom file:

```
orrery-frontend-react:
    build:
      context: ../
      dockerfile: docker/orrery-frontend/Dockerfile
    image: acryldata/orrery-frontend-react:${ORRERY_VERSION:-quickstart}
    env_file: orrery-frontend/env/docker.env
    hostname: orrery-frontend-react
    container_name: orrery-frontend-react
    ports:
      - "9002:9002"
    depends_on:
      - orrery-gms
    volumes:
      - ./my-custom-dir/jaas.conf:/orrery-frontend/conf/jaas.conf
```

And then run `docker-compose up` against your compose file.
