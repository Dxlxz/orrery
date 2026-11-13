# Adding user metadata in Orrery

This guide shares how you can add user metadata in Orrery. Usually you would want to use one of our sources for ingesting user metadata. But if there is no connector for your use case then you would want to use this guide.

:::note

This does not allow you to add new users for Authentication. If you want to add a new user in Orrery for Login please refer to [Adding Users to Orrery](../authentication/guides/add-users.md)

:::

You can look at all aspects supported for users in [CorpUserAspect](../../metadata-models/src/main/pegasus/com/linkedin/metadata/aspect/CorpUserAspect.pdl)

## Using File-Based Ingestion Recipe

Define a JSON File containing your user

```my-user.json
[
    {
        "auditHeader": null,
        "proposedSnapshot": {
            "com.linkedin.pegasus2avro.metadata.snapshot.CorpUserSnapshot": {
                "urn": "urn:li:corpuser:aseem.bansal",
                "aspects": [
                    {
                        "com.linkedin.pegasus2avro.identity.CorpUserInfo": {
                            "active": true,
                            "displayName": {
                                "string": "Aseem Bansal"
                            },
                            "email": "aseem+examples@orrery.dev",
                            "title": {
                                "string": "Software Engineer"
                            },
                            "managerUrn": null,
                            "departmentId": null,
                            "departmentName": null,
                            "firstName": null,
                            "lastName": null,
                            "fullName": {
                                "string": "Aseem Bansal"
                            },
                            "countryCode": null
                        }
                    }
                ]
            }
        }
    }
]
```

Define an [ingestion recipe](https://docs.orrery.dev/docs/metadata-ingestion/#recipes)

```
---
# see https://docs.orrery.dev/docs/generated/ingestion/sources/file for complete documentation
source:
  type: "file"
  config:
    path: "./my-user.json"

# see https://docs.orrery.dev/docs/metadata-ingestion/sink_docs/orrery for complete documentation
sink:
 type: "orrery-rest"
  config:
    server: "http://localhost:8080"
```

Use [Orrery CLI](../cli.md) to do the ingestion.

## Using Rest.li API

```
curl 'http://localhost:8080/entities?action=ingest' -X POST --data '{
	"entity": {
		"value": {
			"com.linkedin.metadata.snapshot.CorpUserSnapshot": {
				"urn": "urn:li:corpuser:aseem.bansal",
				"aspects": [{
					"com.linkedin.identity.CorpUserInfo": {
                        "active": true,
						"displayName": "Aseem Bansal",
						"email": "aseem+example@orrery.dev",
						"title": "Software Engineer",
						"fullName": "Aseem Bansal"
					}
				}]
			}
		}
	}
}'
```
