import React from "react";

import useDocusaurusContext from "@docusaurus/useDocusaurusContext";
import { FilterPage } from "./docs/_components/FilterPage";
import { FastBackwardFilled } from "@ant-design/icons";
const integrations = require("../../../docs/generated/ingestion/integrations.json");
const metadata = integrations.ingestionSources;

function DataProviderComponent() {
  const context = useDocusaurusContext();
  const { siteConfig = {} } = context;
  const connectorCount = Math.floor(metadata.length / 10) * 10;

  return FilterPage(
    siteConfig,
    metadata,
    "Orrery Integrations",
    `Connect to ${connectorCount}+ data and AI systems`,
    false,
    true,
    false,
    `Orrery Connector Directory: ${connectorCount}+ Integrations`,
    `Find every system Orrery integrates with. Browse ${connectorCount}+ Orrery connectors across databases, BI tools, ETL, AI/ML systems, and more.`
  );
}

export default DataProviderComponent;
