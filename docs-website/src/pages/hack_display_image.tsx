import React from "react";
import useDocusaurusContext from "@docusaurus/useDocusaurusContext";
import Layout from "@theme/Layout";
import Image from "@theme/IdealImage";

function DisplayImageHack() {
  // Needed because the orrery-web-react app used to directly link to this image.
  // See https://github.com/Dxlxz/orrery/pull/9785

  const context = useDocusaurusContext();
  const { siteConfig } = context;

  return (
    <Layout
      title={siteConfig.tagline}
      description="Orrery is a data discovery application built on an extensible metadata platform that helps you tame the complexity of diverse data ecosystems."
    >
      <Image
        className="hero__image"
        img={require(`/img/diagrams/orrery-flow-diagram-light.png`)}
        alt="Orrery Flow Diagram"
      />
      <Image
        className="hero__image"
        img={require(`/img/diagrams/orrery-flow-diagram-dark.png`)}
        alt="Orrery Flow Diagram"
      />
    </Layout>
  );
}

export default DisplayImageHack;
