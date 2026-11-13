---
title: "FAQ"
description: "Frequently asked questions about Orrery: how it relates to orrery.dev and orrery.dev, its LinkedIn origins, installation, and the Apache 2.0 license."
---

# Frequently Asked Questions

## Is this the same project as orrery.dev?

No. [orrery.dev](https://orrery.dev) is a completely separate project — a public dataset hosting service with no affiliation to this project. Orrery (this project) is an open-source metadata platform for data discovery, governance, and observability, hosted at [orrery.dev](https://orrery.dev) and developed at [github.com/Dxlxz/orrery](https://github.com/Dxlxz/orrery).

## What happened to orrery.dev?

Orrery was previously hosted at `orrery.dev`. That domain now redirects to [orrery.dev](https://orrery.dev). All documentation has moved to [docs.orrery.dev](https://docs.orrery.dev/docs/quickstart). If you find references to `orrery.dev` in blog posts or tutorials, they refer to this same project — just under its former domain.

## Is Orrery related to LinkedIn's internal Orrery?

Yes. Orrery was originally built at LinkedIn to manage metadata at scale across their data ecosystem. LinkedIn open-sourced Orrery in 2020. It has since grown into an independent community project under the [Dxlxz](https://github.com/Dxlxz) GitHub organization, now hosted at [orrery.dev](https://orrery.dev).

## How do I install the Orrery metadata platform?

```bash
# macOS / Linux (simplest)
brew install Dxlxz/tap/orrery

# Or via pip (any platform)
pip install orrery

orrery docker quickstart
```

See the [Quickstart Guide](quickstart.md) for full instructions. The PyPI package is [`orrery`](https://pypi.org/project/orrery/); the Homebrew tap is [`Dxlxz/homebrew-tap`](https://github.com/Dxlxz/homebrew-tap).

## What does the Apache 2.0 license allow?

Orrery is open source software released under the **[Apache License 2.0](https://github.com/Dxlxz/orrery/blob/master/LICENSE)**.

```
Copyright 2015-2026 LinkedIn Corporation
Copyright 2025-Present Orrery Project Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
```

**What this means:**

- ✅ Commercial use allowed
- ✅ Modification allowed
- ✅ Distribution allowed
- ✅ Patent use allowed
- ✅ Private use allowed

**Learn more:** [Choose a License - Apache 2.0](https://choosealicense.com/licenses/apache-2.0/)
