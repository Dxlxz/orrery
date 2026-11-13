# read-modify-write requires access to the OrreryGraph (RestEmitter is not enough)
from orrery.ingestion.graph.client import OrreryClientConfig, OrreryGraph

gms_endpoint = "http://localhost:8080"
graph = OrreryGraph(OrreryClientConfig(server=gms_endpoint))

# Query multiple aspects from entity
query = """
mutation batchRemoveTerms {
    batchRemoveTerms(
      input: {
        termUrns: ["urn:li:glossaryTerm:rateofreturn"],
        resources: [
          { resourceUrn:"urn:li:dataset:(urn:li:dataPlatform:hdfs,SampleHdfsDataset,PROD)"} ,
          { resourceUrn:"urn:li:dataset:(urn:li:dataPlatform:hive,fct_users_created,PROD)"} ,]
      }
    )
}
"""
result = graph.execute_graphql(query=query)

print(result)
