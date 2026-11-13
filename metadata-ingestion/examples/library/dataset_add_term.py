from typing import List, Union

from orrery.sdk import DatasetUrn, GlossaryTermUrn, OrreryClient


def add_terms_to_dataset(
    client: OrreryClient,
    dataset_urn: DatasetUrn,
    term_urns: List[Union[GlossaryTermUrn, str]],
) -> None:
    """
    Add glossary terms to a dataset.

    Args:
        client: Orrery client to use
        dataset_urn: URN of the dataset to update
        term_urns: List of term URNs or term names to add
    """
    dataset = client.entities.get(dataset_urn)

    for term in term_urns:
        if isinstance(term, str):
            resolved_term_urn = client.resolve.term(name=term)
            dataset.add_term(resolved_term_urn)
        else:
            dataset.add_term(term)

    client.entities.update(dataset)


def main() -> None:
    """
    Main function to add terms to dataset example.
    """
    client = OrreryClient.from_env()

    dataset_urn = DatasetUrn(platform="hive", name="realestate_db.sales", env="PROD")

    # Add terms using both URN and name resolution
    add_terms_to_dataset(
        client=client,
        dataset_urn=dataset_urn,
        term_urns=[
            GlossaryTermUrn("Classification.HighlyConfidential"),
            "PII",  # Will be resolved by name
        ],
    )


if __name__ == "__main__":
    main()
