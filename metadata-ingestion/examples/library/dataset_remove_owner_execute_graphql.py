# read-modify-write requires access to the OrreryGraph (RestEmitter is not enough)
from orrery.ingestion.graph.client import OrreryClientConfig, OrreryGraph

gms_endpoint = "http://localhost:8080"
graph = OrreryGraph(OrreryClientConfig(server=gms_endpoint))

# Query multiple aspects from entity
query = """
mutation batchRemoveOwners {
    batchRemoveOwners(
      input: {
        ownerUrns: ["urn:li:corpuser:jdoe"],
        resources: [
          { resourceUrn:"urn:li:dataset:(urn:li:dataPlatform:hdfs,SampleHdfsDataset,PROD)"} ,
          { resourceUrn:"urn:li:dataset:(urn:li:dataPlatform:hive,fct_users_created,PROD)"} ,]
      }
    )
}
"""
result = graph.execute_graphql(query=query)

print(result)
