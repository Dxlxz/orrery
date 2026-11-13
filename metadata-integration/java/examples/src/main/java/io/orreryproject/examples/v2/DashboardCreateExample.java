package io.orreryproject.examples.v2;

import com.linkedin.common.OwnershipType;
import orrery.client.v2.OrreryClientV2;
import orrery.client.v2.entity.Dashboard;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Example demonstrating how to create a Dashboard using Java SDK V2.
 *
 * <p>This example shows:
 *
 * <ul>
 *   <li>Creating a OrreryClientV2
 *   <li>Building a Dashboard with fluent builder
 *   <li>Adding tags, owners, and custom properties
 *   <li>Upserting to Orrery
 * </ul>
 */
public class DashboardCreateExample {

  public static void main(String[] args)
      throws IOException, ExecutionException, InterruptedException {
    // Create client (use environment variables or pass explicit values)
    OrreryClientV2 client =
        OrreryClientV2.builder()
            .server(System.getenv().getOrDefault("ORRERY_SERVER", "http://localhost:8080"))
            .token(System.getenv("ORRERY_TOKEN")) // Optional
            .build();

    try {
      // Test connection
      if (!client.testConnection()) {
        System.err.println("Failed to connect to Orrery server");
        return;
      }
      System.out.println("✓ Connected to Orrery");

      // Build dashboard with metadata
      Dashboard dashboard =
          Dashboard.builder()
              .tool("looker")
              .id("sales_performance_dashboard")
              .title("Sales Performance Dashboard")
              .description(
                  "Executive dashboard showing key sales metrics including revenue, growth, and regional performance")
              .build();

      System.out.println("✓ Built dashboard with URN: " + dashboard.getUrn());

      // Add tags
      dashboard.addTag("executive").addTag("sales").addTag("production");

      System.out.println("✓ Added 3 tags");

      // Add owners
      dashboard
          .addOwner("urn:li:corpuser:orrery", OwnershipType.BUSINESS_OWNER)
          .addOwner("urn:li:corpuser:data_team", OwnershipType.TECHNICAL_OWNER);

      System.out.println("✓ Added 2 owners");

      // Add custom properties
      dashboard
          .addCustomProperty("team", "sales-operations")
          .addCustomProperty("refresh_schedule", "hourly")
          .addCustomProperty("data_source", "snowflake")
          .addCustomProperty("dashboard_type", "executive");

      System.out.println("✓ Added 4 custom properties");

      // Upsert to Orrery
      client.entities().upsert(dashboard);

      System.out.println("✓ Successfully created dashboard in Orrery!");
      System.out.println("\n  URN: " + dashboard.getUrn());
      System.out.println(
          "  View in Orrery: "
              + client.getConfig().getServer()
              + "/dashboard/"
              + dashboard.getUrn());

    } finally {
      client.close();
    }
  }
}
