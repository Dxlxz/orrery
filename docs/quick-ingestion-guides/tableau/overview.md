---
title: Tableau Overview
description: "Quick start guide for ingesting Tableau workbooks, dashboards, sheets, and lineage into Orrery via the UI ingestion workflow."
---

# Tableau Ingestion Guide: Overview

## What You Will Get Out of This Guide

This guide will help you set up the Tableau connector to begin ingesting metadata into Orrery.

Upon completing this guide, you will have a recurring ingestion pipeline that will extract metadata from Tableau and load it into Orrery. This will include to following Tableau asset types:

- Dashboards
- Sheets
- Embedded DataSource
- Published DataSource
- Custom SQL Table
- Embedded or External Tables
- User
- Workbook
- Tag

The pipeline will also extract:

- **Usage statistics** help you understand top viewed Dashboard/Chart
- **Table- and Column-level lineage** automatically index relationships between datasets and columns

## Next Steps

Continue to the [setup guide](setup.md), where we'll describe the prerequisites.

## Advanced Guides and Reference

If you want to ingest metadata from Tableau using the Orrery CLI, check out the following resources:

- Learn about CLI Ingestion in the [Introduction to Metadata Ingestion](../../../metadata-ingestion/README.md)
- [Tableau Ingestion Source](https://docs.orrery.dev/docs/generated/ingestion/sources/tableau)
