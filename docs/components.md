---
title: "Components"
---

# Orrery Components Overview

The Orrery platform consists of the components shown in the following diagram.

<p align="center">
  <img width="70%"  src="https://raw.githubusercontent.com/Dxlxz/static-assets/main/imgs/orrery-components.png"/>
</p>

## Metadata Store

The Metadata Store is responsible for storing the [Entities & Aspects](https://docs.orrery.dev/docs/metadata-modeling/metadata-model/) comprising the Metadata Graph. This includes
exposing an API for [ingesting metadata](https://docs.orrery.dev/docs/metadata-service#ingesting-entities), [fetching Metadata by primary key](https://docs.orrery.dev/docs/metadata-service#retrieving-entities), [searching entities](https://docs.orrery.dev/docs/metadata-service#search-an-entity), and [fetching Relationships](https://docs.orrery.dev/docs/metadata-service#get-relationships-edges) between
entities. It consists of a Spring Java Service hosting a set of [Rest.li](https://linkedin.github.io/rest.li/) API endpoints, along with
MySQL, Elasticsearch, & Kafka for primary storage & indexing.

Get started with the Metadata Store by following the [Quickstart Guide](https://docs.orrery.dev/docs/quickstart/).

## Metadata Models

Metadata Models are schemas defining the shape of the Entities & Aspects comprising the Metadata Graph, along with the relationships between them. They are defined
using [PDL](https://linkedin.github.io/rest.li/pdl_schema), a modeling language quite similar in form to Protobuf while serializes to JSON. Entities represent a specific class of Metadata
Asset such as a Dataset, a Dashboard, a Data Pipeline, and beyond. Each _instance_ of an Entity is identified by a unique identifier called an `urn`. Aspects represent related bundles of data attached
to an instance of an Entity such as its descriptions, tags, and more. View the current set of Entities supported [here](https://docs.orrery.dev/docs/metadata-modeling/metadata-model#exploring-orrerys-metadata-model).

Learn more about Orrery models Metadata [here](https://docs.orrery.dev/docs/metadata-modeling/metadata-model/).

## Ingestion Framework

The Ingestion Framework is a modular, extensible Python library for extracting Metadata from external source systems (e.g.
Snowflake, Looker, MySQL, Kafka), transforming it into Orrery's [Metadata Model](https://docs.orrery.dev/docs/metadata-modeling/metadata-model/), and writing it into Orrery via
either Kafka or using the Metadata Store Rest APIs directly. Orrery supports an [extensive list of Source connectors](https://docs.orrery.dev/docs/metadata-ingestion/#installing-plugins) to choose from, along with
a host of capabilities including schema extraction, table & column profiling, usage information extraction, and more.

Getting started with the Ingestion Framework is as simple: just define a YAML file and execute the `orrery ingest` command.
Learn more by heading over the [Metadata Ingestion](https://docs.orrery.dev/docs/metadata-ingestion/) guide.

## GraphQL API

The [GraphQL](https://graphql.org/) API provides a strongly-typed, entity-oriented API that makes interacting with the Entities comprising the Metadata
Graph simple, including APIs for adding and removing tags, owners, links & more to Metadata Entities! Most notably, this API is consumed by the User Interface (discussed below) for enabling Search & Discovery, Governance, Observability
and more.

To get started using the GraphQL API, check out the [Getting Started with GraphQL](https://docs.orrery.dev/docs/api/graphql/getting-started) guide.

## User Interface

Orrery comes with a React UI including an ever-evolving set of features to make Discovering, Governing, & Debugging your Data Assets easy & delightful.
For a full overview of the capabilities currently supported, take a look at the [Features](features.md) overview. For a look at what's coming next,
head over to the [Roadmap](https://docs.orrery.dev/docs/roadmap/).

## Learn More

Learn more about the specifics of the [Orrery Architecture](./architecture/architecture.md) in the Architecture Overview. Learn about using & developing the components
of the Platform by visiting the Module READMEs.

## Feedback / Questions / Concerns

We want to hear from you! For any inquiries, including Feedback, Questions, or Concerns, reach out on [Slack](https://orreryspace.slack.com/join/shared_invite/zt-nx7i0dj7-I3IJYC551vpnvvjIaNRRGw#/shared-invite/email)!
