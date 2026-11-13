import React, { useEffect } from "react";

export default function Home() {
  useEffect(() => {
    window.location.href = "https://www.orrery.dev/events";
  }, []);

  return null;
}
