import React, { useEffect } from "react";

export default function Home() {
  useEffect(() => {
    window.location.href = "https://orrery.dev/slack";
  }, []);

  return null;
}
