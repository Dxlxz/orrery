package io.orreryproject.examples.v2;

import com.linkedin.common.OwnershipType;
import orrery.client.v2.OrreryClientV2;
import orrery.client.v2.entity.MLModel;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Example demonstrating how to create an ML Model using Java SDK V2.
 *
 * <p>This example shows:
 *
 * <ul>
 *   <li>Creating a OrreryClientV2
 *   <li>Building an ML Model with fluent builder
 *   <li>Adding training metrics and hyperparameters
 *   <li>Adding tags, owners, and custom properties
 *   <li>Upserting to Orrery
 * </ul>
 */
public class MLModelCreateExample {

  public static void main(String[] args)
      throws IOException, ExecutionException, InterruptedException {
    // Create client (use environment variables or pass explicit values)
    OrreryClientV2 client =
        OrreryClientV2.builder()
            .server(System.getenv().getOrDefault("ORRERY_SERVER", "http://localhost:8080"))
            .token(System.getenv("ORRERY_TOKEN"))
            .build();

    try {
      // Test connection
      if (!client.testConnection()) {
        System.err.println("Failed to connect to Orrery server");
        return;
      }
      System.out.println("Connected to Orrery");

      // Build ML model with basic metadata
      MLModel model =
          MLModel.builder()
              .platform("tensorflow")
              .name("user_churn_predictor")
              .env("PROD")
              .displayName("User Churn Prediction Model")
              .description("XGBoost model predicting user churn probability based on user behavior")
              .build();

      System.out.println("Built ML model with URN: " + model.getUrn());

      // Add training metrics
      model
          .addTrainingMetric("accuracy", "0.94")
          .addTrainingMetric("precision", "0.93")
          .addTrainingMetric("recall", "0.91")
          .addTrainingMetric("f1_score", "0.92")
          .addTrainingMetric("auc_roc", "0.96");

      System.out.println("Added 5 training metrics");

      // Add hyperparameters
      model
          .addHyperParam("learning_rate", "0.01")
          .addHyperParam("max_depth", "6")
          .addHyperParam("n_estimators", "100")
          .addHyperParam("subsample", "0.8")
          .addHyperParam("colsample_bytree", "0.8");

      System.out.println("Added 5 hyperparameters");

      // Add tags
      model.addTag("production").addTag("ml-model").addTag("classification");

      System.out.println("Added 3 tags");

      // Add owners
      model
          .addOwner("urn:li:corpuser:ml_team", OwnershipType.TECHNICAL_OWNER)
          .addOwner("urn:li:corpuser:data_science", OwnershipType.DATA_STEWARD);

      System.out.println("Added 2 owners");

      // Add custom properties
      model
          .addCustomProperty("framework", "XGBoost")
          .addCustomProperty("training_date", "2025-10-15")
          .addCustomProperty("model_version", "1.0.0")
          .addCustomProperty("training_dataset", "user_events_2025_q3");

      System.out.println("Added 4 custom properties");

      // Upsert to Orrery
      client.entities().upsert(model);

      System.out.println("\nSuccessfully created ML model in Orrery!");
      System.out.println("\n  URN: " + model.getUrn());
      System.out.println(
          "  View in Orrery: " + client.getConfig().getServer() + "/mlModel/" + model.getUrn());

    } finally {
      client.close();
    }
  }
}
