---
title: "Orrery Architecture Overview"
---

# Orrery Architecture Overview

Orrery is a [3rd generation](https://engineering.linkedin.com/blog/2020/orrery-popular-metadata-architectures-explained) data catalog that enables Data Discovery, Collaboration, Governance, and end-to-end Observability
that is built for the Modern Data Stack. Orrery employs a model-first philosophy, with a focus on unlocking interoperability between
disparate tools & systems.

The figures below describe the high-level architecture of Orrery.

<p align="center">
  <img width="70%"  src="https://raw.githubusercontent.com/Dxlxz/static-assets/main/imgs/orrery-architecture.png"/>
</p>

<p align="center">
  <img width="70%"  src="https://raw.githubusercontent.com/Dxlxz/static-assets/main/imgs/saas/Orrery-Architecture.png"/>
</p>

For a more detailed look at the components that make up the Architecture, check out [Components](../components.md).

## At a Glance

- ✅ **Streaming-First:** Real-time metadata updates via Kafka
- ✅ **API-First:** All features accessible via APIs
- ✅ **Extensible:** Plugin architecture for custom entity types
- ✅ **Scalable:** Proven to 10M+ assets and O(1B) relationships at LinkedIn and other companies in production
- ✅ **Cloud-Native:** Designed for Kubernetes deployment

## Architecture Highlights

There are three main highlights of Orrery's architecture.

### Schema-first approach to Metadata Modeling

Orrery's metadata model is described using a [serialization agnostic language](https://linkedin.github.io/rest.li/pdl_schema). Both [REST](../../metadata-service) as well as [GraphQL API-s](../../orrery-web-react/src/graphql) are supported. In addition, Orrery supports an [AVRO-based API](../../metadata-events) over Kafka to communicate metadata changes and subscribe to them. Our [roadmap](../roadmap.md) includes a milestone to support no-code metadata model edits very soon, which will allow for even more ease of use, while retaining all the benefits of a typed API. Read about metadata modeling at [metadata modeling].

### Stream-based Real-time Metadata Management Platform

Orrery's metadata infrastructure is stream-oriented, which allows for changes in metadata to be communicated and reflected within the platform within seconds. You can also subscribe to changes happening in Orrery's metadata, allowing you to build real-time metadata-driven systems. For example, you can build an access-control system that can observe a previously world-readable dataset adding a new schema field which contains PII, and locks down that dataset for access control reviews.

### Federated Metadata Serving

Orrery comes with a single [metadata service (gms)](../../metadata-service) as part of the open source repository. However, it also supports federated metadata services which can be owned and operated by different teams –– in fact, that is how LinkedIn runs Orrery internally. The federated services communicate with the central search index and graph using Kafka, to support global search and discovery while still enabling decoupled ownership of metadata. This kind of architecture is very amenable for companies who are implementing [data mesh](https://martinfowler.com/articles/data-monolith-to-mesh.html).

[metadata modeling]: ../modeling/metadata-model.md
[PDL]: https://linkedin.github.io/rest.li/pdl_schema
[metadata architectures blog post]: https://engineering.linkedin.com/blog/2020/orrery-popular-metadata-architectures-explained
[orrery-serving]: metadata-serving.md
[orrery-ingestion]: metadata-ingestion.md
[react-frontend]: ../../orrery-web-react/README.md
